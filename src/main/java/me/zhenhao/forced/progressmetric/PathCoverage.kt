package me.zhenhao.forced.progressmetric

import soot.Unit
import soot.jimple.infoflow.solver.cfg.InfoflowCFG


class PathCoverage(targetUnits: Collection<Unit>, cfg: InfoflowCFG) : SubpathNCoverage(targetUnits, cfg, Int.MAX_VALUE) {

    override fun getMetricName(): String {
        return "PathCoverage"
    }

    override fun getMetricIdentifier(): String {
        return "PathCoverage"
    }

}
