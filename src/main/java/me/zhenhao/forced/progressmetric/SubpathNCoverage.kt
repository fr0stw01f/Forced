package me.zhenhao.forced.progressmetric

import java.util.ArrayList
import java.util.HashSet
import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import me.zhenhao.forced.decisionmaker.server.history.ClientHistory


class SubpathNCoverage(val targetUnits: Collection<Unit>, val cfg: InfoflowCFG) : IProgressMetric {

    private val fragmentLength: Int = 2
    private val coveredPathFragment = HashSet<List<Triple<Int, Unit, Boolean>>>()

    override fun update(history: ClientHistory): Int {
        val trace = history.pathTrace
        var retval = 0

        // coverage of path fragments of length = fragmentLength
        if (fragmentLength > 0 && trace.size > fragmentLength) {
            retval += (0..trace.size-fragmentLength-1)
                    .filter { update(trace.subList(it, it + fragmentLength)) }
                    .count()
        } else if (update(trace)) { //path coverage
            retval++
        }

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
