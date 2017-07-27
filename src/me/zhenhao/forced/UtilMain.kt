package me.zhenhao.forced

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.HashSet

import soot.Scene
import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import me.zhenhao.forced.bootstrap.InstanceIndependentCodePosition
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.frameworkevents.manager.FrameworkEventManager
import me.zhenhao.forced.shared.SharedClassesSettings


object UtilMain {

    private val BLACKLISTED_APKS = "." + File.separator + "files" + File.separator + "blacklistedAPKs.txt"

    fun convertUnitsToIndependentCodePositions(units: Set<Unit>, cfg: InfoflowCFG): Set<InstanceIndependentCodePosition> {
        val positions = HashSet<InstanceIndependentCodePosition>()

        for (unit in units) {
            //now we have access to the CFG, check if target is reachable:
            if (!cfg.isReachable(unit) || cfg.getMethodOf(unit) == null) {
                LoggerHelper.logEvent(MyLevel.LOGGING_POINT, "target is not statically reachable!")
                continue
            }

            val methodSig = cfg.getMethodOf(unit).signature
            val lineNum = generateLineNumberOfUnit(unit, cfg)
            val statement = unit.toString()
            val position = InstanceIndependentCodePosition(methodSig, lineNum, statement)
            positions.add(position)
        }

        return positions
    }


    fun convertIndependentCodePositionToUnit(codePos: InstanceIndependentCodePosition): Unit? {
        var unit: Unit? = null

        val sm = Scene.v().getMethod(codePos.methodSignature)
        if (sm != null) {
            var currentLineNum = 0
            val unitIt = sm.activeBody.units.iterator()
            while (unitIt.hasNext()) {
                val currentUnit = unitIt.next()
                if (currentLineNum == codePos.lineNumber && currentUnit.toString() == codePos.statement)
                    unit = currentUnit
                currentLineNum++
            }
        }

        return unit
    }


    private fun generateLineNumberOfUnit(unit: Unit, cfg: InfoflowCFG): Int {
        val sm = cfg.getMethodOf(unit) ?: return -1

        var lineNum = 0
        val unitIt = sm.activeBody.units.iterator()
        while (unitIt.hasNext()) {
            val currentUnit = unitIt.next()
            // Is this the statement
            if (unit === currentUnit)
                return lineNum
            lineNum++
        }

        return -1
    }


    val blacklistedAPKs: Set<String>
        get() {
            val blacklisted = HashSet<String>()

            try {
                BufferedReader(FileReader(BLACKLISTED_APKS)).use { br ->
                    var line = br.readLine()
                    while (line != null) {
                        if (!line.startsWith("%"))
                            blacklisted.add(line)
                        line = br.readLine()
                    }
                }
            } catch (ex: Exception) {
                LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                ex.printStackTrace()
                System.exit(-1)
            }

            return blacklisted
        }


    fun writeToFile(fileName: String, content: String) {
        val outputFile = File(fileName)
        val fw: FileWriter?
        var bw: BufferedWriter? = null
        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            fw = FileWriter(outputFile.absoluteFile, true)
            bw = BufferedWriter(fw)
            bw.write(content)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            try {
                bw!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun initAndCleanBranchTrackingFiles() {
        try {
            val remoteFile = SharedClassesSettings.BRANCH_TRACKING_DIR_PATH + "file_counter.txt"
            val localFile = FrameworkOptions.resultsDir + "file_counter.txt"

            val file = File(localFile)

            val bw = file.bufferedWriter()
            bw.write(0.toString())

            bw.close()

            FrameworkEventManager.eventManager.pushFile(localFile, remoteFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        FrameworkEventManager.eventManager.removeFilesByPattern(SharedClassesSettings.BRANCH_TRACKING_DIR_PATH + "bt_*")
    }
}
