package de.tu_darmstadt.sse.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel

class StartApkEvent(private val packageName: String) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        //		String shellCmd = String.format("am start -n %s/.%s", packageName, launchableActivity);
        val shellCmd = String.format("monkey -p %s -c android.intent.category.LAUNCHER 1", packageName)
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LoggerHelper.logEvent(MyLevel.OPEN_APK, "APK opened")
        } catch (e: Exception) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "not able to start apk: " + e.message)
            e.printStackTrace()
        }

        return null
    }

    override fun toString(): String {
        return "StartApkEvent"
    }
}
