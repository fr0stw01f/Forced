package me.zhenhao.forced.dynamiccfg.utils

import java.io.BufferedReader
import java.io.FileReader
import java.util.HashSet

import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel


object FileUtils {

    fun textFileToLineSet(fileName: String): Set<String> {
        val analysesNames = HashSet<String>()
        try {
            val br = BufferedReader(FileReader(fileName))
            br.use { br ->
                var line = br.readLine()
                while (line != null) {
                    analysesNames.add(line)
                    line = br.readLine()
                }
            }
        } catch (ex: Exception) {
            LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
            ex.printStackTrace()
            System.exit(-1)
        }

        return analysesNames
    }

}
