package de.tu_darmstadt.sse.progressmetric

import java.util.ArrayList
import java.util.HashSet
import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import de.tu_darmstadt.sse.decisionmaker.server.history.ClientHistory


class SubpathNCoverage(targetUnits: Collection<Unit>,
                       val fragmentLength: Int) : IProgressMetric {

    private val coveredPathFragment = HashSet<List<Pair<Unit, Boolean>>>()

    lateinit private var cfg: InfoflowCFG

    constructor(targetUnits: Collection<Unit>, cfg: InfoflowCFG) : this(targetUnits, 2) {
        this.cfg = cfg
    }

    override fun update(history: ClientHistory): Int {
        val trace = history.getPathTrace()
        var retval = 0

        if (fragmentLength > 0 && trace.size > fragmentLength) { //coverage of path fragments of length = fragmentLength
            (0..trace.size - fragmentLength - 1)
                    .filter { update(trace.subList(it, it + fragmentLength)) }
                    .forEach { retval++ }
        } else if (update(trace)) { //path coverage
            retval++
        }

        history.setProgressValue(getMetricIdentifier(), getNumCovered())
        return getNumCovered()
    }

    private fun update(l: List<Pair<Unit, Boolean>>): Boolean {
        var retval = false
        if (coveredPathFragment.add(ArrayList(l))) {
            retval = true
        }
        return retval
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

    override fun initialize() {
    }

    override fun setCurrentTargetLocation(currentTargetLocation: Unit) {
    }
}
