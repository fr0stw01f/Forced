package me.zhenhao.forced.frameworkevents.broadcastevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.frameworkevents.FrameworkEvent
import me.zhenhao.forced.frameworkevents.GenericReceiver

class FakeBroadcastEvent(private val receiverClassName: String?, val actionName: String?, private val mimeType: String?, private val packageName: String?) : FrameworkEvent() {


    override fun onEventReceived(device: IDevice): Any? {
        val shellCmd = prepareStartService()
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LoggerHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), shellCmd))
        } catch (e: Exception) {
            //			LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, "not able to send a broadcast: " + e.getMessage());
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
        val other = obj as FakeBroadcastEvent?

        val otherActionName = if (other!!.actionName != null) other.actionName else "10"
        val thisActionname = this.actionName ?: "10"
        val otherReceiverClassName = other.receiverClassName ?: "20"
        val thisReceiverClassName = this.receiverClassName ?: "20"
        val otherPackageName = other.packageName ?: "30"
        val thisPackageName = this.packageName ?: "30"
        val otherMimeType = other.mimeType ?: "40"
        val thisMimeType = this.mimeType ?: "40"

        if (otherActionName != thisActionname)
            return false
        if (otherReceiverClassName != thisReceiverClassName)
            return false
        if (otherPackageName != thisPackageName)
            return false
        if (otherMimeType != thisMimeType)
            return false
        return true
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += receiverClassName?.hashCode() ?: 10
        hashCode += actionName?.hashCode() ?: 20
        hashCode += packageName?.hashCode() ?: 30
        hashCode += mimeType?.hashCode() ?: 40
        return hashCode
    }

    private fun prepareStartService(): String {
        if (mimeType != null) {
            return String.format("am startservice --es \"className\" %s" + " --es \"action\" %s --es \"task\" \"broadcast\" --es \"mimeType\" %s -n %s/%s",
                    receiverClassName, actionName, mimeType, packageName, UtilInstrumenter.COMPONENT_CALLER_SERVICE_HELPER)
        } else {
            return String.format("am startservice --es \"className\" %s" + " --es \"action\" %s --es \"task\" \"broadcast\"  -n %s/%s",
                    receiverClassName, actionName, packageName, UtilInstrumenter.COMPONENT_CALLER_SERVICE_HELPER)
        }
    }

    override fun toString(): String {
        return "FakeBroadcastEvent"
    }


}
