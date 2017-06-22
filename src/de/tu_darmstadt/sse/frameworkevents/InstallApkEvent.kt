package de.tu_darmstadt.sse.frameworkevents

import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.appinstrumentation.UtilInstrumenter
import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel

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
