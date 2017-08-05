package me.zhenhao.forced.progressmetric

import java.util.HashSet
import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory


class BranchCoverage(val targetUnits: Collection<Unit>, val cfg: InfoflowCFG) : IProgressMetric {

    private val branchChoices = HashSet<Triple<Int, Unit, Boolean>>()

    override fun update(history: ClientHistory): Int {
        branchChoices.addAll(history.branchChoices)
        return branchChoices.size
    }

    fun getNumCovered(): Int {
        return branchChoices.size
    }

    override fun getMetricName(): String {
        return "BranchCoverage"
    }

    override fun getMetricIdentifier(): String {
        return "BranchCoverage"
    }

    override fun initialize() {}

    override fun setCurrentTargetLocation(currentTargetLocation: Unit) {}
}
