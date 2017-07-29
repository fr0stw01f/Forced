package me.zhenhao.forced.frameworkevents

import java.util.concurrent.TimeUnit

import com.android.ddmlib.IDevice

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


class OnClickEvent(onClickListenerClass: String, private val packageName: String?) : FrameworkEvent() {
    private val onClickListenerClass: String?

    init {
        //in case of an inner class, we have to escape the $ sign
        this.onClickListenerClass = onClickListenerClass.replace("$", "\\$")
    }

    override fun onEventReceived(device: IDevice): Any? {
        val shellCmd = String.format("am startservice --es \"className\" \"%s\"" + " --es \"task\" \"onClick\" -n %s/%s",
                onClickListenerClass, packageName, UtilInstrumenter.HELPER_SERVICE_FOR_COMPONENT_CALLER)
        try {
            device.executeShellCommand(shellCmd, GenericReceiver(), 10000, TimeUnit.MILLISECONDS)
            LogHelper.logEvent(MyLevel.ADB_EVENT, adbEventFormat(toString(), onClickListenerClass!!))
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
        val other = obj as OnClickEvent?

        val otherOnClickListenerClass = if (other!!.onClickListenerClass != null) other.onClickListenerClass else "10"
        val thisOnClickListenerClass = this.onClickListenerClass ?: "10"
        val otherPackageName = other.packageName ?: "20"
        val thisPackageName = this.packageName ?: "20"

        if (otherOnClickListenerClass != thisOnClickListenerClass)
            return false
        if (thisPackageName != otherPackageName)
            return false
        return true
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += packageName?.hashCode() ?: 10
        hashCode += onClickListenerClass?.hashCode() ?: 20
        return hashCode
    }

    override fun toString(): String {
        return "(Fake) OnClickEvent"
    }

}
