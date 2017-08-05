package me.zhenhao.forced.decisionmaker

import java.io.File
import java.util.HashMap
import java.util.HashSet

import soot.Unit
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.commandlinelogger.MyLevel
import me.zhenhao.forced.decisionmaker.analysis.FuzzyAnalysis
import me.zhenhao.forced.dynamiccfg.utils.FileUtils
import me.zhenhao.forced.progressmetric.IProgressMetric


class DecisionMakerConfig {

    val analysesNames = FileUtils.textFileToLineSet(ANALYSES_FILENAME)
    val progressMetricNames = FileUtils.textFileToLineSet(METRICS_FILENAME)

    val allAnalyses: MutableSet<FuzzyAnalysis> = HashSet()
    val nameToAnalysis: MutableMap<String, FuzzyAnalysis> = HashMap()
    val progressMetrics: MutableSet<IProgressMetric> = HashSet()
    val allTargetLocations: MutableSet<Unit> = HashSet()

    lateinit var backwardsCFG: BackwardsInfoflowCFG
        private set


    fun initialize(targetLocations: Set<Unit>): Boolean {
        var successful = registerFuzzyAnalyses()
        allTargetLocations.addAll(targetLocations)
        if (!successful)
            return false
        successful = registerProgressMetrics()
        return successful
    }

    fun initializeCFG() {
        val forwardCFG = InfoflowCFG()
        backwardsCFG = BackwardsInfoflowCFG(forwardCFG)
    }


    private fun registerFuzzyAnalyses(): Boolean {
        val registeredAnalyses = analysesNames
        registeredAnalyses
                .filterNot { it.startsWith("%") }
                .forEach {
                    try {
                        val analysisClass = Class.forName(it)
                        val defaultConstructor = analysisClass.getConstructor()
                        defaultConstructor.isAccessible
                        val constructorObject = defaultConstructor.newInstance() as? FuzzyAnalysis ?:
                                throw RuntimeException("There is a problem in files/analysesNames.txt!")
                        val analysis = constructorObject

                        allAnalyses.add(analysis)
                        nameToAnalysis.put(analysis.getAnalysisName(), analysis)
                        LogHelper.logEvent(MyLevel.ANALYSIS, "[ANALYSIS-TYPE] " + it)
                    } catch (ex: Exception) {
                        LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                        ex.printStackTrace()
                        return false
                    }
                }
        return true
    }


    private fun registerProgressMetrics(): Boolean {
        val registeredMetrics = progressMetricNames
        registeredMetrics
                .filterNot { it.startsWith("%") }
                .forEach {
                    try {
                        val metricClass = Class.forName(it)
                        val defaultConstructor = metricClass.getConstructor(Collection::class.java, InfoflowCFG::class.java)

                        if (!defaultConstructor.isAccessible)
                            defaultConstructor.isAccessible = true

                        val constructorObject = defaultConstructor.newInstance(allTargetLocations, backwardsCFG)
                                as? IProgressMetric ?:
                                throw RuntimeException("There is a problem in the files/metricsNames.txt file!")
                        val metric = constructorObject
                        LogHelper.logEvent(MyLevel.ANALYSIS, "[METRIC-TYPE] " + it)

                        // currently, there can be only a single target
                        if (allTargetLocations.size != 1)
                            throw RuntimeException("There can be only 1 target location per run")
                        val target = allTargetLocations.iterator().next()
                        if (backwardsCFG.getMethodOf(target) != null) {
                            metric.setCurrentTargetLocation(target)

                            // initialize the metric, otherwise it is empty!
                            metric.initialize()
                            progressMetrics.add(metric)
                        } else {
                            LogHelper.logEvent(MyLevel.LOGGING_POINT, "target is not statically reachable!")
                            return false
                        }
                    } catch (ex: Exception) {
                        LogHelper.logEvent(MyLevel.EXCEPTION_ANALYSIS, ex.message)
                        ex.printStackTrace()
                        System.exit(-1)
                    }
                }
        return true
    }


    fun getAnalysisByName(name: String): FuzzyAnalysis? {
        return nameToAnalysis[name]
    }

    companion object {
        private val ANALYSES_FILENAME = "." + File.separator + "files" + File.separator + "analysesNames.txt"
        private val METRICS_FILENAME = "." + File.separator + "files" + File.separator + "metricsNames.txt"
    }

}
