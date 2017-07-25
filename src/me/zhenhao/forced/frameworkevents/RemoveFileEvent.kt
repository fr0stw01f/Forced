package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import java.util.concurrent.TimeUnit


class RemoveFileEvent(private val fileName: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {
		val shellCmd = String.format("rm " +  fileName)
		try {
			device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
			LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), "file removed..."))
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return null
	}

	override fun toString(): String {
		return "RemoveFileEvent"
	}
}
