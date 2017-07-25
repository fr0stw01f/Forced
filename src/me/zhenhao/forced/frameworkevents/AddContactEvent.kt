package me.zhenhao.forced.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


class AddContactEvent(private val packageName: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {
		val shellCmd = String.format("am startservice --es \"task\" \"addContact\" -n %s/%s",
					packageName, UtilInstrumenter.HELPER_SERVICE_FOR_COMPONENT_CALLER)
//		val shellCmd = String.format("am start -a android.intent.action.INSERT -t vnd.android.cursor.dir/contact -e name %s -e phone %s",
//				"\'Zhenhao Tang\'", "765-4091238")
		try {
			device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
			LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), "contact added..."))
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return null
	}

	override fun toString(): String {
		return "AddContactEvent"
	}
}
