package de.tu_darmstadt.sse.frameworkevents

import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel

class PhoneRestartEvent : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        try {
            device.reboot(null)
            LoggerHelper.logEvent(MyLevel.RESTART, "App restarted event sent")
        } catch (e: Exception) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "Not able to reboot device...: " + e.message)
            e.printStackTrace()
        }

        return null
    }

    override fun toString(): String {
        return "PhoneRestartEvent"
    }
}
