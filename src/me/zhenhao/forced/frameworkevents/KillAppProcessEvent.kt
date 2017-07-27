package me.zhenhao.forced.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


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
