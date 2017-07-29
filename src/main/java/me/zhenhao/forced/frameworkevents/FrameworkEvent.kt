package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice


abstract class FrameworkEvent {
    abstract fun onEventReceived(device: IDevice): Any?

    fun adbEventFormat(eventType: String, eventCmd: String): String {
        return String.format("%s || %s", eventType, eventCmd)
    }
}
