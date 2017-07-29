package me.zhenhao.forced.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


class ActivityEvent(private val packageName: String?, private val activityPath: String?) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        //just start the onCreate for now; we do not care whether the bundle is null
        val shellCmd = String.format("am start -W -n %s/%s", packageName, activityPath)
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
        val other = obj as ActivityEvent?

        val otherPackageName = if (other!!.packageName != null) other.packageName else "10"
        val thisPackageName = this.packageName ?: "10"
        val otherActivityPath = other.activityPath ?: "20"
        val thisActivityPath = this.activityPath ?: "20"

        if (otherActivityPath != thisActivityPath)
            return false
        if (otherPackageName != thisPackageName)
            return false
        return true
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += packageName!!.hashCode()
        hashCode += activityPath!!.hashCode()
        return hashCode
    }

    override fun toString(): String {
        return "ActivityEvent"
    }
}
