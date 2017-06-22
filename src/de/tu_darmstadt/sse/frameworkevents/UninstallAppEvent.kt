package de.tu_darmstadt.sse.frameworkevents

import com.android.ddmlib.IDevice
import com.android.ddmlib.InstallException

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel


class UninstallAppEvent(private val packageName: String) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        try {
            device.uninstallPackage(packageName)
        } catch (e: InstallException) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, e.message)
            e.printStackTrace()
            System.exit(-1)
        }

        return null
    }

    override fun toString(): String {
        return "UninstallApkEvent"
    }

}
