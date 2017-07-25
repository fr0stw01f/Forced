package me.zhenhao.forced.frameworkevents

import com.android.ddmlib.IDevice


class PullFileEvent(private val src: String, private val dst: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {

		device.pullFile(src, dst)

		return null
	}

}
