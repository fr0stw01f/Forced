package me.zhenhao.forced.frameworkevents.manager

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatReceiverTask
import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.frameworkevents.*
import me.zhenhao.forced.frameworkevents.broadcastevents.*
import soot.SootClass
import soot.Unit
import soot.jimple.infoflow.android.axml.AXmlNode
import soot.jimple.infoflow.android.manifest.ProcessManifest
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG
import java.util.*
import java.util.concurrent.TimeUnit


class FrameworkEventManager {

    private var device: IDevice? = null
    private var adb: AndroidDebugBridge? = null

    fun connectToAndroidDevice() {
        LogHelper.logEvent(MyLevel.RUNTIME, "Connecting to ADB...")
        if (adb == null) {
            AndroidDebugBridge.init(false)
            //        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(
            //                FrameworkOptions.platformTools + File.separator + "adb", true);
            adb = AndroidDebugBridge.createBridge()
        }

        waitForDevice()
        this.device = getDevice(FrameworkOptions.devicePort)
        //this.device = adb.getDevices()[0];
        if (this.device == null) {
            LogHelper.logEvent(MyLevel.EXCEPTION_RUNTIME, String.format("Device with port %s not found! -- retry it", FrameworkOptions.devicePort))
            connectToAndroidDevice()
        }
        LogHelper.logEvent(MyLevel.RUNTIME, "Successfully connected to ADB...")
    }

    fun sendEvent(event: FrameworkEvent): Any? {
        if (device!!.isOffline) {
            LogHelper.logWarning("Device is offline! Trying to restart it...")
            connectToAndroidDevice()
        }

        return event.onEventReceived(device!!)
    }

    fun installApp(packageName: String) {
        sendEvent(InstallApkEvent(packageName))
    }

    fun startApp(packageName: String) {
        sendEvent(StartApkEvent(packageName))
    }

    fun startActivity(packageName: String, activityName: String) {
        sendEvent(StartActivityEvent(packageName, activityName))
    }

    fun startService(packageName: String, servicePath: String) {
        sendEvent(ServiceEvent(packageName, servicePath))
    }

    fun killAppProcess(packageName: String) {
        LogHelper.logEvent(MyLevel.RUNTIME, "killing application")
        sendEvent(KillAppProcessEvent(packageName))
    }

    fun uninstallAppProcess(packageName: String) {
        sendEvent(UninstallAppEvent(packageName))
    }

    fun pushFuzzyFiles(dirPath: String) {
        sendEvent(PushFuzzyFilesEvent(dirPath))
    }

    fun pullFile(src: String, dst: String) {
        sendEvent(PullFileEvent(src, dst))
    }

    fun pushFile(src: String, dst: String) {
        sendEvent(PushFileEvent(src, dst))
    }

    fun removeFile(fileName: String) {
        sendEvent(RemoveFileEvent(fileName))
    }

    fun removeFilesByPattern(pattern: String) {
        sendEvent(RemoveFilesByPatternEvent(pattern))
    }

    fun addContacts(packageName: String) {
        sendEvent(AddContactEvent(packageName))
    }

    fun extractInitialEventsForReachingTarget(targetLocation: Unit, backwardsCFG: BackwardsInfoflowCFG,
                                              manifest: ProcessManifest): Set<FrameworkEvent> {
        val headUnits = getAllInitialMethodCalls(targetLocation, backwardsCFG)
        return getAndroidEventsFromManifest(backwardsCFG, headUnits, manifest)
    }

    private fun getAllInitialMethodCalls(targetLocation: Unit, backwardsCFG: BackwardsInfoflowCFG): Set<Unit> {
        val headUnits = HashSet<Unit>()
        val reachedUnits = HashSet<Unit>()
        val worklist = LinkedList<Unit>()
        var previousUnit: Unit? = null

        worklist.add(targetLocation)

        while (!worklist.isEmpty()) {
            // get the front element
            val currentUnit = worklist.removeFirst()

            if (reachedUnits.contains(currentUnit)) {
                previousUnit = currentUnit
                continue
            } else
                reachedUnits.add(currentUnit)

            val currentMethod = backwardsCFG.getMethodOf(currentUnit)
            // we reached the head unit
            if (currentMethod.declaringClass.toString() == "dummyMainClass") {
                if (previousUnit == null)
                    throw RuntimeException("there should be a previous unit")

                headUnits.add(previousUnit)
                continue
            }

            // in case we reached the start of the method (vice verse in backward analysis)
            if (backwardsCFG.isExitStmt(currentUnit)) {
                val sm = backwardsCFG.getMethodOf(currentUnit)
                // first: get all callers
                val callers = backwardsCFG.getCallersOf(sm)
                // get the predecessors (aka succs of cfg) of the callers and add them to the worklist
                callers
                        .map { backwardsCFG.getSuccsOf(it) }
                        .flatMap { it }
                        .forEach { worklist.addFirst(it) }
                previousUnit = currentUnit
                // there is no need for further progress
                continue
            }

            val nextUnits = backwardsCFG.getSuccsOf(currentUnit)
            for (unit in nextUnits)
                worklist.addFirst(unit)
            previousUnit = currentUnit
        }

        return headUnits
    }

    private fun getAndroidEventsFromManifest(backwardsCFG: BackwardsInfoflowCFG, headUnits: Set<Unit>,
                                             manifest: ProcessManifest): Set<FrameworkEvent> {
        val events = HashSet<FrameworkEvent>()
        for (head in headUnits) {
            val sm = backwardsCFG.getMethodOf(head)
            val sc = sm.declaringClass
            val superClass = sc.superclass
            val interfaceClasses = sc.interfaces

            if (superClass.name == "android.app.Service") {
                val packageName = manifest.packageName
                val servicePath = sc.name
                events.add(ServiceEvent(packageName, servicePath))
            } else if (superClass.name == "android.content.BroadcastReceiver") {
                for (receiver in manifest.receivers) {
                    if (receiver.hasAttribute("name")) {
                        val receiverName = receiver.getAttribute("name").value as String
                        // now we have to find the correct name of the receiver class
                        val fullyQualifiedReceiverName = getFullyQualifiedName(manifest, receiverName)

                        if (sc.name == fullyQualifiedReceiverName)
                            handleBroadcastReceiver(fullyQualifiedReceiverName, receiver, manifest, events)
                    }
                }
                if (events.isEmpty())
                    LogHelper.logEvent(MyLevel.TODO,
                            "generateAndroidEvents: maybe a dynamic generated broadcast?")
            } else if (superClass.name == "android.app.Activity") {
                //is it the main launchable activity; if yes, we do not have to add any android event since
                //we are opening the app anyway
                if (!isLaunchableActivity(sc, manifest)) {
                    val packageName = manifest.packageName
                    val activityPath = sc.name
                    events.add(ActivityEvent(packageName, activityPath))
                } else
                    continue
            } else {
                //check for listeners (interfaces)
                interfaceClasses
                        .filter { it.name == "android.view.View\$OnClickListener" }
                        .forEach { events.add(OnClickEvent(sc.name, manifest.packageName)) }
                if (events.isEmpty())
                    LogHelper.logEvent(MyLevel.TODO,
                            "generateAndroidEvents: did not find a proper event for head: " + head.toString())
            }

        }
        return events
    }

    fun handleBroadcastReceiver(fullyQualifiedReceiverName: String, receiver: AXmlNode,
                                manifest: ProcessManifest, events: MutableSet<FrameworkEvent>) {
        for (child in receiver.children) {
            if (child.tag == "intent-filter") {
                val actions = HashSet<String>()
                val mimeTypes = HashSet<String>()
                for (child2 in child.children) {
                    if (child2.tag == "action") {
                        val actionName = child2.getAttribute("name").value as String
                        actions.add(actionName)
                    } else if (child2.tag == "data") {
                        val attr = child2.getAttribute("mimeType")
                        if (attr != null)
                            mimeTypes.add(attr.value as String)
                    }
                }

                var mimeType: String? = null
                if (mimeTypes.size > 1) {
                    LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS,
                            "There is more than 1 data element in the intent-filter")
                    mimeType = mimeTypes.iterator().next()
                } else if (mimeTypes.size == 1)
                    mimeType = mimeTypes.iterator().next()

                actions.mapTo(events) { getCorrectBroadCast(it, mimeType, fullyQualifiedReceiverName, manifest) }
            } else {
                // no intent defined in the manifest; maybe dynamically defined during runtime
                events.add(FakeBroadcastEvent(fullyQualifiedReceiverName, null, null, manifest.packageName))
            }
            // we try to call it anyway
        }
    }

    private fun getFullyQualifiedName(manifest: ProcessManifest, componentName: String): String {
        val fullyQualifiedName: String
        if (componentName.startsWith(".")) {
            fullyQualifiedName = manifest.packageName + componentName
        } else if (componentName.contains(".")) {
            fullyQualifiedName = componentName
        } else {
            fullyQualifiedName = manifest.packageName + "." + componentName
        }   // not documented, but still working
        // fully qualified name
        return fullyQualifiedName
    }

    private fun isLaunchableActivity(sc: SootClass, manifest: ProcessManifest): Boolean {
        val launchableActivities = manifest.launchableActivities

        return launchableActivities.any { it ->
            if (it.hasAttribute("name")) {
                var activityName = it.getAttribute("name").value as String
                activityName = getFullyQualifiedName(manifest, activityName)

                if (activityName == sc.name)
                    return true
            }
            return false
        }

    }

    private fun getCorrectBroadCast(actionName: String, mimeType: String?, fullyQualifiedReceiverName: String,
                                    manifest: ProcessManifest): FrameworkEvent {
        val event: FrameworkEvent?

        if (actionName.equals("android.provider.telephony.SMS_RECEIVED", ignoreCase = true))
            event = SMSReceivedEvent()
        else if (actionName.equals("android.intent.action.NEW_OUTGOING_CALL", ignoreCase = true))
            event = OutgoingCallEvent()
        else if (actionName.equals("android.intent.action.NEW_INCOMING_CALL", ignoreCase = true))
            event = IncomingCallEvent()
        else if (actionName.equals("android.intent.action.BOOT_COMPLETED", ignoreCase = true)
                || actionName.equals("android.app.action.ACTION_PASSWORD_CHANGED", ignoreCase = true)
                || actionName.equals("android.app.action.ACTION_PASSWORD_EXPIRING", ignoreCase = true)
                || actionName.equals("android.app.action.ACTION_PASSWORD_FAILED", ignoreCase = true)
                || actionName.equals("android.app.action.ACTION_PASSWORD_SUCCEEDED", ignoreCase = true)
                || actionName.equals("android.app.action.DEVICE_ADMIN_DISABLED", ignoreCase = true)
                || actionName.equals("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED", ignoreCase = true)
                || actionName.equals("android.app.action.DEVICE_ADMIN_ENABLED", ignoreCase = true)
                || actionName.equals("android.app.action.DEVICE_OWNER_CHANGED", ignoreCase = true)
                || actionName.equals("android.app.action.INTERRUPTION_FILTER_CHANGED", ignoreCase = true)
                || actionName.equals("android.app.action.LOCK_TASK_ENTERING", ignoreCase = true)
                || actionName.equals("android.app.action.LOCK_TASK_EXITING", ignoreCase = true)
                || actionName.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED", ignoreCase = true)
                || actionName.equals("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED", ignoreCase = true)
                || actionName.equals("android.app.action.NOTIFICATION_POLICY_CHANGED", ignoreCase = true)
                || actionName.equals("android.app.action.PROFILE_PROVISIONING_COMPLETE", ignoreCase = true)
                || actionName.equals("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED", ignoreCase = true)
                || actionName.equals("android.bluetooth.adapter.action.DISCOVERY_STARTED", ignoreCase = true)
                || actionName.equals("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.adapter.action.SCAN_MODE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.adapter.action.STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.ACL_CONNECTED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.ACL_DISCONNECTED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.BOND_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.CLASS_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.FOUND", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.NAME_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.PAIRING_REQUEST", ignoreCase = true)
                || actionName.equals("android.bluetooth.device.action.UUID", ignoreCase = true)
                || actionName.equals("android.bluetooth.devicepicker.action.DEVICE_SELECTED", ignoreCase = true)
                || actionName.equals("android.bluetooth.devicepicker.action.LAUNCH", ignoreCase = true)
                || actionName.equals("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT", ignoreCase = true)
                || actionName.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.hardware.action.NEW_PICTURE", ignoreCase = true)
                || actionName.equals("android.hardware.action.NEW_VIDEO", ignoreCase = true)
                || actionName.equals("android.hardware.hdmi.action.OSD_MESSAGE", ignoreCase = true)
                || actionName.equals("android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS", ignoreCase = true)
                || actionName.equals("android.intent.action.ACTION_POWER_CONNECTED", ignoreCase = true)
                || actionName.equals("android.intent.action.ACTION_POWER_DISCONNECTED", ignoreCase = true)
                || actionName.equals("android.intent.action.ACTION_SHUTDOWN", ignoreCase = true)
                || actionName.equals("android.intent.action.AIRPLANE_MODE", ignoreCase = true)
                || actionName.equals("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.BATTERY_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.BATTERY_LOW", ignoreCase = true)
                || actionName.equals("android.intent.action.BATTERY_OKAY", ignoreCase = true)
                || actionName.equals("android.intent.action.BOOT_COMPLETED", ignoreCase = true)
                || actionName.equals("android.intent.action.CAMERA_BUTTON", ignoreCase = true)
                || actionName.equals("android.intent.action.CONFIGURATION_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.CONTENT_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.DATA_SMS_RECEIVED", ignoreCase = true)
                || actionName.equals("android.intent.action.DATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.DEVICE_STORAGE_LOW", ignoreCase = true)
                || actionName.equals("android.intent.action.DEVICE_STORAGE_OK", ignoreCase = true)
                || actionName.equals("android.intent.action.DOCK_EVENT", ignoreCase = true)
                || actionName.equals("android.intent.action.DOWNLOAD_COMPLETE", ignoreCase = true)
                || actionName.equals("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED", ignoreCase = true)
                || actionName.equals("android.intent.action.DREAMING_STARTED", ignoreCase = true)
                || actionName.equals("android.intent.action.DREAMING_STOPPED", ignoreCase = true)
                || actionName.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE", ignoreCase = true)
                || actionName.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE", ignoreCase = true)
                || actionName.equals("android.intent.action.FETCH_VOICEMAIL", ignoreCase = true)
                || actionName.equals("android.intent.action.GTALK_CONNECTED", ignoreCase = true)
                || actionName.equals("android.intent.action.GTALK_DISCONNECTED", ignoreCase = true)
                || actionName.equals("android.intent.action.HEADSET_PLUG", ignoreCase = true)
                || actionName.equals("android.intent.action.HEADSET_PLUG", ignoreCase = true)
                || actionName.equals("android.intent.action.INPUT_METHOD_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.LOCALE_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.MANAGE_PACKAGE_STORAGE", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_BAD_REMOVAL", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_BUTTON", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_CHECKING", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_EJECT", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_MOUNTED", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_NOFS", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_REMOVED", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_SCANNER_FINISHED", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_SCANNER_SCAN_FILE", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_SCANNER_STARTED", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_SHARED", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_UNMOUNTABLE", ignoreCase = true)
                || actionName.equals("android.intent.action.MEDIA_UNMOUNTED", ignoreCase = true)
                || actionName.equals("android.intent.action.MY_PACKAGE_REPLACED", ignoreCase = true)
                || actionName.equals("android.intent.action.NEW_OUTGOING_CALL", ignoreCase = true)
                || actionName.equals("android.intent.action.NEW_VOICEMAIL", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_ADDED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_DATA_CLEARED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_FIRST_LAUNCH", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_FULLY_REMOVED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_INSTALL", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_NEEDS_VERIFICATION", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_REMOVED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_REPLACED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_RESTARTED", ignoreCase = true)
                || actionName.equals("android.intent.action.PACKAGE_VERIFIED", ignoreCase = true)
                || actionName.equals("android.intent.action.PHONE_STATE", ignoreCase = true)
                || actionName.equals("android.intent.action.PROVIDER_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.PROXY_CHANGE", ignoreCase = true)
                || actionName.equals("android.intent.action.REBOOT", ignoreCase = true)
                || actionName.equals("android.intent.action.SCREEN_OFF", ignoreCase = true)
                || actionName.equals("android.intent.action.SCREEN_ON", ignoreCase = true)
                || actionName.equals("android.intent.action.TIMEZONE_CHANGED", ignoreCase = true)
                || actionName.equals("android.intent.action.TIME_SET", ignoreCase = true)
                || actionName.equals("android.intent.action.TIME_TICK", ignoreCase = true)
                || actionName.equals("android.intent.action.UID_REMOVED", ignoreCase = true)
                || actionName.equals("android.intent.action.USER_PRESENT", ignoreCase = true)
                || actionName.equals("android.intent.action.WALLPAPER_CHANGED", ignoreCase = true)
                || actionName.equals("android.media.ACTION_SCO_AUDIO_STATE_UPDATED", ignoreCase = true)
                || actionName.equals("android.media.AUDIO_BECOMING_NOISY", ignoreCase = true)
                || actionName.equals("android.media.RINGER_MODE_CHANGED", ignoreCase = true)
                || actionName.equals("android.media.SCO_AUDIO_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.media.VIBRATE_SETTING_CHANGED", ignoreCase = true)
                || actionName.equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION", ignoreCase = true)
                || actionName.equals("android.media.action.HDMI_AUDIO_PLUG", ignoreCase = true)
                || actionName.equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION", ignoreCase = true)
                || actionName.equals("android.net.conn.BACKGROUND_DATA_SETTING_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.conn.CONNECTIVITY_CHANGE", ignoreCase = true)
                || actionName.equals("android.net.nsd.STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.scoring.SCORER_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.scoring.SCORE_NETWORKS", ignoreCase = true)
                || actionName.equals("android.net.wifi.NETWORK_IDS_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.wifi.RSSI_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.wifi.SCAN_RESULTS", ignoreCase = true)
                || actionName.equals("android.net.wifi.STATE_CHANGE", ignoreCase = true)
                || actionName.equals("android.net.wifi.WIFI_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE", ignoreCase = true)
                || actionName.equals("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE", ignoreCase = true)
                || actionName.equals("android.net.wifi.p2p.PEERS_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.wifi.p2p.STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.wifi.p2p.THIS_DEVICE_CHANGED", ignoreCase = true)
                || actionName.equals("android.net.wifi.supplicant.CONNECTION_CHANGE", ignoreCase = true)
                || actionName.equals("android.net.wifi.supplicant.STATE_CHANGE", ignoreCase = true)
                || actionName.equals("android.nfc.action.ADAPTER_STATE_CHANGED", ignoreCase = true)
                || actionName.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED", ignoreCase = true)
                || actionName.equals("android.os.action.POWER_SAVE_MODE_CHANGED", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SIM_FULL", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SMS_CB_RECEIVED", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SMS_DELIVER", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SMS_RECEIVED", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SMS_REJECTED", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.WAP_PUSH_DELIVER", ignoreCase = true)
                || actionName.equals("android.provider.Telephony.WAP_PUSH_RECEIVED", ignoreCase = true)
                || actionName.equals("android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED", ignoreCase = true)
                || actionName.equals("android.speech.tts.engine.TTS_DATA_INSTALLED", ignoreCase = true))
            event = FakeBroadcastEvent(fullyQualifiedReceiverName, actionName, mimeType, manifest.packageName)
        else
            event = RealBroadcastEvent(null, actionName, mimeType)//all these actions are taken from android-sdks/platforms/android-23/data/broadcast_actions.txt

        return event
    }

    private fun waitForDevice() {
        val start = System.currentTimeMillis()
        while (!adb!!.hasInitialDeviceList()) {
            val timeLeft = start + ADB_CONNECT_TIMEOUT_MS - System.currentTimeMillis()

            if (timeLeft <= 0) {
                break
            }

            try {
                Thread.sleep(ADB_CONNECT_TIME_STEP_MS)
            } catch (ex: Exception) {
                LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                ex.printStackTrace()
            }

        }

        if (!adb!!.hasInitialDeviceList()) {
            LogHelper.logEvent(MyLevel.EXCEPTION_RUNTIME,
                    "NOT POSSIBLE TO CONNECT TO ADB -- giving up and closing program!")
            System.exit(-1)
        }

        var count = 0
        while (adb!!.devices.isEmpty()) {
            try {
                Thread.sleep(5000)
                LogHelper.logEvent(MyLevel.RUNTIME, "Cannot find a device...")
                count++
            } catch (e: InterruptedException) {
                LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, e.message)
                e.printStackTrace()
                AndroidDebugBridge.terminate()
            }

            if (count > 50) {
                LogHelper.logEvent(MyLevel.RUNTIME,
                                "After 100 seconds not able to find an Android device. Shutting down...")
                AndroidDebugBridge.terminate()
            }
        }
    }

    private fun getDevice(devicePort: String): IDevice? {
        for (iDev in adb!!.devices) {
            if (iDev.serialNumber.contains(devicePort)) {
                LogHelper.logEvent(MyLevel.RUNTIME, "Successfully connected to emulator: " + iDev.serialNumber)
                return iDev
            }
        }
        return null
    }

    fun startLogcatCrashViewer() {
        try {
            device!!.executeShellCommand("logcat -c", GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val logCatReceiverTask = LogCatReceiverTask(device!!)
        logCatReceiverTask.addLogCatListener { msgList ->
            msgList.forEach { msg ->
                if (msg.message.contains("VFY:"))
                    LogHelper.logEvent(MyLevel.VMCRASH,
                            String.format("############### VM CRASHED ###############\n%s",msg.toString()))
            }
        }

        Thread(logCatReceiverTask).start()
    }

    companion object {
        private val ADB_CONNECT_TIMEOUT_MS: Long = 5000
        private val ADB_CONNECT_TIME_STEP_MS = ADB_CONNECT_TIMEOUT_MS / 10

        private var frameworkEventManager: FrameworkEventManager? = null

        val eventManager: FrameworkEventManager
            get() {
                if (frameworkEventManager == null)
                    frameworkEventManager = FrameworkEventManager()
                return frameworkEventManager!!
            }
    }
}
