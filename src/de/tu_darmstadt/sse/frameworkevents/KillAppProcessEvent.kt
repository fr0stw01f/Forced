package de.tu_darmstadt.sse.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel


class KillAppProcessEvent(private val packageName: String) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        val shellCmd = String.format("am force-stop %s", packageName)
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "not able to kill app: " + e.message)
            e.printStackTrace()
        }

        return null
    }

    override fun toString(): String {
        return "KillApkEvent"
    }

}
