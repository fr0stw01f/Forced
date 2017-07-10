package me.zhenhao.forced.decisionmaker.analysis.filefuzzer

import java.util.HashSet

import soot.jimple.infoflow.handlers.ResultsAvailableHandler
import soot.jimple.infoflow.results.InfoflowResults
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.sharedclasses.networkconnection.FileFormat


class FileFuzzerResultsAvailableHandler(private val valuesToFuzz: MutableMap<Int, FileFormat>) : ResultsAvailableHandler {
    internal var codePositionManager = CodePositionManager.codePositionManagerInstance

    internal var PROPERTIES_SINKS: Set<String> = object : HashSet<String>() {
        init {
            add("<java.util.Properties: void load(java.io.InputStream)>")
            add("<java.util.Properties: void store(java.io.OutputStream,java.lang.String)>")
        }
    }

    internal var DIR_SINKS: Set<String> = object : HashSet<String>() {
        init {
            add("<java.io.File: boolean mkdirs()>")
            add("<java.io.File: java.io.File[] listFiles()>")
            add("<java.io.File: boolean isDirectory()>")
        }
    }

    override fun onResultsAvailable(cfg: IInfoflowCFG, results: InfoflowResults) {
        for (sinkInfo in results.results.keySet()) {
            //all sources
            val sourceInfo = results.results.get(sinkInfo)
            val sinkStmt = sinkInfo.sink
            if (sinkStmt.containsInvokeExpr()) {
                val inv = sinkStmt.invokeExpr
                val sm = inv.method

                //check for properties files
                if (PROPERTIES_SINKS.contains(sm.signature)) {
                    sourceInfo
                            .map { codePositionManager.getCodePositionForUnit(it.source) }
                            .forEach { valuesToFuzz.put(it.id, FileFormat.PROPERTIES) }
                } else if (DIR_SINKS.contains(sm.signature)) {
                    sourceInfo
                            .map { codePositionManager.getCodePositionForUnit(it.source) }
                            .forEach { valuesToFuzz.put(it.id, FileFormat.DIRECTORY) }
                } else
                    LoggerHelper.logEvent(MyLevel.TODO, "WE NEED TO ADD A NEW FILE FORMAT: " + sinkInfo)//directory
            } else
                throw RuntimeException("this should not happen in FileFuzzerResultsAvailableHandler")
        }
    }
}