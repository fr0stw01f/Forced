package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice
import com.android.ddmlib.InstallException

import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


class UninstallAppEvent(private val packageName: String) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        try {
            device.uninstallPackage(packageName)
        } catch (e: InstallException) {
            LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, e.message)
            e.printStackTrace()
            System.exit(-1)
        }

        return null
    }

    override fun toString(): String {
        return "UninstallApkEvent-$packageName"
    }

}
