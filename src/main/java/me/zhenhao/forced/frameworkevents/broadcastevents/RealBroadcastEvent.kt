package me.zhenhao.forced.frameworkevents.broadcastevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.frameworkevents.FrameworkEvent
import me.zhenhao.forced.frameworkevents.GenericReceiver


class RealBroadcastEvent(private val extras: Map<String, Any>?, private val action: String?, private val mimeType: String?) : FrameworkEvent() {

    override fun onEventReceived(device: IDevice): Any? {
        val shellCmd = String.format("am broadcast %s -a %s %s", prepareExtras(extras), action, prepareMimeType(mimeType))
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LogHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), shellCmd))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun prepareExtras(extras: Map<String, Any>?): String {
        val sb = StringBuilder()
        if (extras == null || extras.keys.isEmpty())
            return ""
        else {
            for ((key, value) in extras) {
                if (value is Boolean)
                    sb.append(String.format(" --ez %s %s ", key, value))
                else if (value is String)
                    sb.append(String.format(" --es %s %s ", key, value))
                else if (value is Int)
                    sb.append(String.format(" --ei %s %s ", key, value))
                else if (value is Long)
                    sb.append(String.format(" --el %s %s ", key, value))
                else if (value is Float)
                    sb.append(String.format(" --ef %s %s ", key, value))
            }
        }
        return sb.toString()
    }

    private fun prepareMimeType(mimeType: String?): String {
        if (mimeType == null)
            return ""
        else
            return String.format(" -t %s ", mimeType)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as RealBroadcastEvent?

        val otherMimeType = if (other!!.mimeType != null) other.mimeType else "10"
        val thisMimeType = this.mimeType ?: "10"
        val otherAction = other.action ?: "20"
        val thisAction = this.action ?: "20"

        if (otherMimeType != thisMimeType)
            return false
        if (otherAction != thisAction)
            return false
        if (other.extras !== this.extras)
            return false
        return true
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += mimeType?.hashCode() ?: 10
        hashCode += action?.hashCode() ?: 20
        hashCode += extras?.hashCode() ?: 30
        return hashCode
    }

    override fun toString(): String {
        return "RealBroadcastEvent"
    }
}
