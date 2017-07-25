package me.zhenhao.forced.frameworkevents

import java.io.File

import com.android.ddmlib.IDevice

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.shared.SharedClassesSettings


class PushFuzzyFilesEvent(private val dirPath: String) : FrameworkEvent() {

	override fun onEventReceived(device: IDevice): Any? {
		val fileDir = File(dirPath)
		if (!fileDir.exists())
			throw RuntimeException("The directory of the files which need to be pushed onto the device is not correct!")
		for (file in fileDir.listFiles()!!) {
			val remoteFilePath = SharedClassesSettings.FUZZY_FILES_DIR_PATH + file.name

			try {
				device.pushFile(file.absolutePath, remoteFilePath)
			} catch (e: Exception) {
				LoggerHelper.logEvent(MyLevel.EXCEPTION_RUNTIME, "Problem with pushing files onto device: " + e.message)
				e.printStackTrace()
			}

		}

		return null
	}

}
