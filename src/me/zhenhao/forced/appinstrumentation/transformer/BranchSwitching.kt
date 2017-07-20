package me.zhenhao.forced.appinstrumentation.transformer

import soot.Body
import soot.Unit
import soot.Value
import soot.jimple.IfStmt
import soot.jimple.IntConstant
import soot.jimple.Jimple
import soot.jimple.Stmt
import java.util.*
import kotlin.collections.HashSet


class BranchSwitching : AbstractInstrumentationTransformer() {

    private val branchTargetStmts = HashSet<String>()

    //val predicates = ArrayList<Pair<Int, Boolean>>()
    val positives  = ArrayList<Int>()
    val negatives  = ArrayList<Int>()
    val switches   = ArrayList<Int>()
    var counter    = 0

    val executed   = HashSet<Stmt>()

    override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
        // Do not instrument methods in framework classes
        if (!canInstrumentMethod(body.method))
            return

        loadSwitches()

        //important to use snapshotIterator here
        val iterator = body.units.snapshotIterator()

        while (iterator.hasNext()) {
            val unit = iterator.next()

            if (unit is IfStmt && !unit.hasTag(InstrumentedCodeTag.name)) {
                var condition: Value = unit.condition
                if (positives.contains(counter)) {
                    condition = Jimple.v().newEqExpr(IntConstant.v(0), IntConstant.v(0))
                    executed.add(unit.target)
                } else if (negatives.contains(counter)) {
                    condition = Jimple.v().newEqExpr(IntConstant.v(0), IntConstant.v(1))
                    executed.add(getUnitAfter(body, unit))
                } else if (switches.contains(counter)) {
                    // todo nothing for now
                }

                //predicates.add(counter++, condition)
                switchBranch(unit, condition)
            }
        }

    }

    private fun loadSwitches() {
        positives.add(0)
        positives.add(1)
        positives.add(2)
    }


    private fun switchBranch(ifStmt: IfStmt, condition: Value) {
        ifStmt.condition = condition
    }

    private fun getUnitAfter(body: Body, unit: Unit): Stmt {
        var u = body.units.first
        while (u != unit)
            u = body.units.getSuccOf(u)

        val unitAfter = body.units.getSuccOf(unit)
        return unitAfter as Stmt
    }

}
