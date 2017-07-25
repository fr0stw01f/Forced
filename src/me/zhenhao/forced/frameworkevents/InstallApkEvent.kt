package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel

class InstallApkEvent(private val packageName: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {
		val deployedApkPath = UtilInstrumenter.SOOT_OUTPUT_DEPLOYED_APK
		try {
			val res = device.installPackage(deployedApkPath, true)
			if (res != null && !res.isEmpty())
				LoggerHelper.logWarning("Not able to install apk $packageName. Error message: $res")
		} catch (e: Exception) {
			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "Not able to install apk " + packageName)
			e.printStackTrace()
		}

		return null
	}

	override fun toString(): String {
		return "InstallAEvent"
	}

}
