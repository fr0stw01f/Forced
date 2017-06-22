package de.tu_darmstadt.sse.frameworkevents.broadcastevents

import java.util.concurrent.ThreadLocalRandom

import com.android.ddmlib.EmulatorConsole
import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel
import de.tu_darmstadt.sse.frameworkevents.FrameworkEvent


class IncomingCallEvent : FrameworkEvent() {
    override fun onEventReceived(device: IDevice): Any? {
        var senderNumber = ""
        for (i in 0..7)
            senderNumber += ThreadLocalRandom.current().nextInt(0, 9 + 1)

        val emulatorConsole = EmulatorConsole.getConsole(device)
        LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), String.format("incomingCall(%s)", senderNumber)))
        //call a random number
        emulatorConsole?.call(senderNumber)
        //let it ring for 3 seconds
        try {
            Thread.sleep(3000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //cancel call
        emulatorConsole?.cancelCall(senderNumber)
        return null
    }

    override fun toString(): String {
        return "IncomingCallEvent"
    }
}
