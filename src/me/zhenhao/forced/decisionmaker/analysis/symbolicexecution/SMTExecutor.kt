package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution

import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTProgram
import java.io.*
import java.util.*
import java.util.regex.Pattern


class SMTExecutor(private val smtPrograms: Set<SMTProgram>, private val Z3script: File) {

    fun createSMTFile(): Set<File?> {
        val allSMTProgramFiles = HashSet<File?>()
        for (smtProgram in smtPrograms) {
            var smtFile: File? = null
            var fos: FileOutputStream? = null
            try {
                smtFile = File.createTempFile("Z3SMT.txt", null)
                fos = FileOutputStream(smtFile!!.path)
                val output = smtProgram.toString().toByteArray()
                fos.write(output)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    fos!!.close()
                } catch (ex: Exception) {
                    System.err.println("File-handle not closed properly")
                }

            }
            allSMTProgramFiles.add(smtFile)
        }

        return allSMTProgramFiles
    }


    fun executeZ3str2ScriptAndExtractLoggingPointValue(smtFile: File): String? {
        var stdInput: BufferedReader? = null
        var stdError: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec(String.format("%s -f %s", Z3script.absolutePath, smtFile.absolutePath))
            stdInput = BufferedReader(InputStreamReader(p.inputStream))
            stdError = BufferedReader(InputStreamReader(p.errorStream))
            val pattern = "loggingPoint_0.*\"(.*)\""
            val r = Pattern.compile(pattern)

            var line = stdInput.readLine()
            while (line != null) {
                val m = r.matcher(line)
                if (m.find()) {
                    return m.group(1)
                }
                line = stdInput.readLine()
            }
        } catch (e: IOException) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, e.message)
            e.printStackTrace()
            System.exit(-1)
        } finally {
            try {
                stdInput!!.close()
                stdError!!.close()
            } catch (e: IOException) {
                System.err.println("BufferedReader not properly closed!")
            }

        }
        return null
    }


    fun executeZ3str2ScriptAndExtractValue(smtFile: File, value: String): String? {
        var stdInput: BufferedReader? = null
        var stdError: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec(String.format("%s -f %s", Z3script.absolutePath, smtFile.absolutePath))
            stdInput = BufferedReader(InputStreamReader(p.inputStream))
            stdError = BufferedReader(InputStreamReader(p.errorStream))
            val pattern = value.replace("$", "\\$") + "\\_0.*\"(.*)\""
            val r = Pattern.compile(pattern)

            var line = stdInput.readLine()
            while (line != null) {
                val m = r.matcher(line)
                if (m.find()) {
                    return m.group(1)
                }
                line = stdInput.readLine()
            }
        } catch (e: IOException) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, e.message)
            e.printStackTrace()
            System.exit(-1)
        } finally {
            try {
                stdInput!!.close()
                stdError!!.close()
            } catch (e: IOException) {
                System.err.println("BufferedReader not properly closed!")
            }

        }
        return null
    }
}
