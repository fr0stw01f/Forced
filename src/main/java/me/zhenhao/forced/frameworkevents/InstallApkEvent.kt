package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice

import me.zhenhao.forced.appinstrumentation.InstrumenterUtil
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel

class InstallApkEvent(private val packageName: String) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        val deployedApkPath = InstrumenterUtil.SOOT_OUTPUT_DEPLOYED_APK
        try {
            device.installPackage(deployedApkPath, true)
        } catch (e: Exception) {
            LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "Not able to install apk " + packageName)
            e.printStackTrace()
        }

        return null
    }

    override fun toString(): String {
        return "InstallAEvent"
    }

}
