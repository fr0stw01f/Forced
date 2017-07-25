package me.zhenhao.forced.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


class StartActivityEvent(private val packageName: String, private val activityName: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {
		val shellCmd = String.format("am start -n %s/.%s", packageName, activityName)
		try {
			device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
			LoggerHelper.logEvent(MyLevel.START_ACTIVITY, String.format("started activity %s/%s", packageName, activityName))
		} catch (e: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, String.format("not able to start activity %s/%s: ", packageName, activityName, e.message))
			e.printStackTrace()
		}

		return null
	}

	override fun toString(): String {
		return "StartActivityEvent"
	}

}
