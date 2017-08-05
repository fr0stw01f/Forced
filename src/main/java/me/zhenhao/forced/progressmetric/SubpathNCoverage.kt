package me.zhenhao.forced.progressmetric

import java.util.ArrayList
import java.util.HashSet
import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory


open class SubpathNCoverage(val targetUnits: Collection<Unit>, val cfg: InfoflowCFG,
                            var fragmentLength: Int = 2) : IProgressMetric {

    private val coveredPathFragment = HashSet<List<Triple<Int, Unit, Boolean>>>()

    constructor(targetUnits: Collection<Unit>, cfg: InfoflowCFG) : this(targetUnits, cfg, 2) {
        this.fragmentLength = fragmentLength
    }

    override fun update(history: ClientHistory): Int {
        val trace = history.pathTrace

        // general cases
        if (fragmentLength > 0 && trace.size > fragmentLength)
            (0..trace.size-fragmentLength-1).forEach { update(trace.subList(it, it + fragmentLength)) }
        // special case: path coverage
        else
            update(trace)

        history.setProgressValue(getMetricIdentifier(), getNumCovered())
        return getNumCovered()
    }

    private fun update(l: List<Triple<Int, Unit, Boolean>>): Boolean {
        return coveredPathFragment.add(ArrayList(l))
    }

    fun getNumCovered(): Int {
        return coveredPathFragment.size
    }

    override fun getMetricName(): String {
        return "SubpathNCoverage"
    }

    override fun getMetricIdentifier(): String {
        return "SubpathNCoverage"
    }

    override fun initialize() {}

    override fun setCurrentTargetLocation(currentTargetLocation: Unit) {}
}
