package de.tu_darmstadt.sse.frameworkevents

import java.io.File

import com.android.ddmlib.IDevice

import de.tu_darmstadt.sse.commandlinelogger.LoggerHelper
import de.tu_darmstadt.sse.commandlinelogger.MyLevel
import de.tu_darmstadt.sse.sharedclasses.SharedClassesSettings


class PushFilesEvent(private val dirPath: String) : FrameworkEvent() {

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
