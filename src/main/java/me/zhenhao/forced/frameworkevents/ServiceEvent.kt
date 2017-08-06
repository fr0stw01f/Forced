package me.zhenhao.forced.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


class ServiceEvent(private val packageName: String?, private val servicePath: String?) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        //just start the onCreate for now; we do not care whether the bundle is null
        val shellCmd = String.format("am startservice %s/%s", packageName, servicePath)
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LogHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), shellCmd))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as ServiceEvent?

        val otherServicePath = if (other!!.servicePath != null) other.servicePath else "10"
        val thisServicePath = this.servicePath ?: "10"
        val otherPackageName = other.packageName ?: "20"
        val thisPackgeName = this.packageName ?: "20"

        if (otherServicePath != thisServicePath)
            return false
        if (otherPackageName != thisPackgeName)
            return false
        return true
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += packageName?.hashCode() ?: 10
        hashCode += servicePath?.hashCode() ?: 20
        return hashCode
    }

    override fun toString(): String {
        return "ServiceStartEvent-$servicePath"
    }

}
