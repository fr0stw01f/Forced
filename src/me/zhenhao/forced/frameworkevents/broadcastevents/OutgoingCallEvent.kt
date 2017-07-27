package me.zhenhao.forced.frameworkevents.broadcastevents

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.frameworkevents.FrameworkEvent
import me.zhenhao.forced.frameworkevents.GenericReceiver


class OutgoingCallEvent : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        var numberToCall = ""
        for (i in 0..7)
            numberToCall += ThreadLocalRandom.current().nextInt(0, 9 + 1)
        val shellCmd = String.format("am broadcast -a android.intent.action.NEW_OUTGOING_CALL --es PHONE_NUMBER %s", numberToCall)
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), shellCmd))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null

    }

    override fun toString(): String {
        return "OutgoingCallEvent"
    }
}
