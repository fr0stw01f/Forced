package de.tu_darmstadt.sse.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel


class ActivityEvent(private val packageName: String?, private val activityPath: String?) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        //just start the onCreate for now; we do not care whether the bundle is null
        val shellCmd = String.format("am start -W -n %s/%s", packageName, activityPath)
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), shellCmd))
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
