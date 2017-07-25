package me.zhenhao.forced.frameworkevents.broadcastevents

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import com.android.ddmlib.EmulatorConsole
import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.frameworkevents.FrameworkEvent


class SMSReceivedEvent : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {
		var senderNumber = ""
		for (i in 0..7)
			senderNumber += ThreadLocalRandom.current().nextInt(0, 9 + 1)
		val message = UUID.randomUUID().toString().replace("-", "")

		val emulatorConsole = EmulatorConsole.getConsole(device)
		if (emulatorConsole == null) {
			LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), "ERROR: Could not send SMS"))
			return null
		}

		emulatorConsole.sendSms(senderNumber, message)
		LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), String.format("SMS received: Nr: %s | Msg: %s", senderNumber, message)))
		return null
	}

	override fun toString(): String {
		return "SMSReceivedEvent"
	}
}
