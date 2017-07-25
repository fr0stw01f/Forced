package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice


class PushFileEvent(private val src: String, private val dst: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {

		device.pushFile(src, dst)

		return null
	}

}
