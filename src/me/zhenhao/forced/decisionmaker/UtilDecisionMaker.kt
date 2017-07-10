package me.zhenhao.forced.decisionmaker

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import me.zhenhao.forced.commandlinelogger.LoggerHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import soot.Scene
import soot.Unit
import soot.jimple.Stmt
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.regex.Pattern


object UtilDecisionMaker {

    private val TARGET_METHODS_FILENAME = "." + File.separator + "files" + File.separator + "targetMethods.txt"

    fun extractAllTargetLocations(): Set<Unit> {
        //extract all logging points from file
        val targetLocationsTmp = HashSet<String>()

        val targetMethods = HashSet<String>()
        val allTargetLocations = HashSet<Unit>()

        try {
            BufferedReader(FileReader(TARGET_METHODS_FILENAME)).use { br ->
                var line = br.readLine()
                while (line != null) {
                    targetLocationsTmp.add(line)
                    line = br.readLine()
                }
            }
        } catch (ex: Exception) {
            LoggerHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
            ex.printStackTrace()
            System.exit(-1)
        }

        targetMethods.addAll(targetLocationsTmp)

        if (!targetLocationsTmp.isEmpty()) {

            val applicationClasses = Scene.v().applicationClasses
            applicationClasses
                    .filter {
                        //no need to look into our code
                        UtilInstrumenter.isAppDeveloperCode(it)
                    }
                    .flatMap { it.methods }
                    .filter { it.hasActiveBody() }
                    .map { it.retrieveActiveBody() }
                    .map { it.units.iterator() }
                    .forEach {
                        while (it.hasNext()) {
                            val curUnit = it.next()
                            if (curUnit is Stmt) {
                                val statement = curUnit

                                if (statement.containsInvokeExpr()) {
                                    val invExpr = statement.invokeExpr
                                    val invokeExprMethodSignature = invExpr.method.signature

                                    // for test only
                                    if (invokeExprMethodSignature.contains("loadUrl"))
                                        allTargetLocations.add(curUnit)

                                    for (targetLocation in targetLocationsTmp) {
                                        //we accept all classes
                                        if (targetLocation.startsWith("<*:")) {
                                            val pattern = "<.:\\s(.*)\\s(.*)\\((.*)\\)>"
                                            val r = Pattern.compile(pattern)

                                            val m = r.matcher(targetLocation)
                                            if (m.find()) {
                                                if (m.group(1) == invExpr.method.returnType.toString() && m.group(2) == invExpr.method.name)
                                                    allTargetLocations.add(curUnit)
                                            }
                                        } else if (targetLocation == invokeExprMethodSignature)
                                            allTargetLocations.add(curUnit)
                                    }
                                }
                            }
                        }
                    }
        }

        return allTargetLocations
    }

}
