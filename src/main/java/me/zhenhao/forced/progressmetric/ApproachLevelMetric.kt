package me.zhenhao.forced.progressmetric

import java.util.HashSet
import java.util.LinkedList

import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG

import com.google.common.collect.HashBasedTable

import me.zhenhao.forced.appinstrumentation.InstrumenterUtil
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory
import me.zhenhao.forced.shared.util.Pair


class ApproachLevelMetric(private val targetUnits: Collection<Unit>, private val cfg: InfoflowCFG) : IProgressMetric {

    private val targetBasedDistanceMap = HashBasedTable.create<Unit, Unit, Int>()
    private var bestSoFar: Int? = Integer.MAX_VALUE
    private var currentTargetLocation: Unit? = null

    private data class ApproachLevelItem(val currentUnit: Unit, val approachLevel: Int)

    override fun initialize() {
        if (currentTargetLocation == null)
            throw RuntimeException("we have to have a target!")

        // init for each target unit
        targetUnits
                //check if this method is reachable; otherwise we can not create an inter-procedural CFG
                .filter { cfg.isReachable(it) }
                .forEach { singleTarget ->
                    val reachedUnits = HashSet<Unit>()
                    val worklist = LinkedList<ApproachLevelItem>()

                    worklist.add(ApproachLevelItem(singleTarget, Integer.MAX_VALUE))

                    outer@ while (!worklist.isEmpty()) {
                        // get front element
                        val curItem = worklist.removeAt(0)
                        val currentUnit = curItem.currentUnit
                        var currentMetricValue = curItem.approachLevel

                        // Do not in circles
                        if (!reachedUnits.add(currentUnit)) {
                            continue
                        }

                        // If we already have a better approach level from another path, do not continue here
                        if (targetBasedDistanceMap.contains(singleTarget, currentUnit)) {
                            val oldMetricValue = targetBasedDistanceMap.get(singleTarget, currentUnit)
                            if (oldMetricValue > currentMetricValue)
                                continue
                        }

                        // We decrease the approach level for every statement that we travel farther
                        // away from the target location
                        targetBasedDistanceMap.put(singleTarget, currentUnit, --currentMetricValue)

                        when {
                            // in case we reached the start of the method (vice verse in backward analysis)
                            cfg.isStartPoint(currentUnit) -> {
                                val sm = cfg.getMethodOf(currentUnit)
                                cfg.getCallersOf(sm)
                                        .flatMap { cfg.getPredsOf(it) }
                                        .mapTo(worklist) { ApproachLevelItem(it, currentMetricValue) }
                            }
                            cfg.isExitStmt(currentUnit) -> {
                                val sm = cfg.getMethodOf(currentUnit)

                                cfg.getCallersOf(sm)
                                        .flatMap { cfg.getReturnSitesOfCallAt(it) }
                                        .forEach {
                                            targetBasedDistanceMap.put(singleTarget, it, currentMetricValue)
                                            worklist.add(ApproachLevelItem(it, currentMetricValue))
                                        }
                                // no need for further progress
                                continue@outer
                            }
                            // in case of a non-api call
                            cfg.isCallStmt(currentUnit) -> {
                                cfg.getCalleesOfCallAt(currentUnit)
                                        .filter { InstrumenterUtil.isAppDeveloperCode(it.declaringClass) }
                                        .flatMap { cfg.getStartPointsOf(it) }
                                        .filter { cfg.getMethodOf(it) != null }
                                        .mapTo(worklist) { ApproachLevelItem(it, currentMetricValue) }
                            }
                        }

                        val nextUnits = cfg.getPredsOf(currentUnit)
                        nextUnits.mapTo(worklist) { ApproachLevelItem(it, currentMetricValue) }
                    }
                }
    }

    private fun getBestApproachLevel(path: Collection<Unit>): Pair<Unit, Int> {
        val retval = Pair<Unit, Int>(null, Integer.MAX_VALUE)
        for (unit in path) {
            val distance = targetBasedDistanceMap.get(currentTargetLocation, unit)
            //in case we are not able to extract the the distance information, we take the old one
            if (distance == null) {
                retval.first = unit
                retval.setSecond(retval.second)
            } else if (distance < retval.second) {
                retval.first = unit
                retval.second = distance
            }

        }
        return retval
    }

    override fun update(history: ClientHistory): Int {
        val value = getBestApproachLevel(history.codePositions).second
        bestSoFar = java.lang.Math.min(bestSoFar!!, value)
        // Set progress value
        history.setProgressValue(getMetricIdentifier(), value)
        return value
    }

    override fun getMetricName(): String {
        return "ApproachLevel"
    }

    override fun getMetricIdentifier(): String {
        return "ApproachLevel"
    }

    override fun setCurrentTargetLocation(currentTargetLocation: Unit) {
        this.currentTargetLocation = currentTargetLocation
    }
}
