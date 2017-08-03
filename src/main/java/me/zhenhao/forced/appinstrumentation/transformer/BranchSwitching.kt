package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import soot.Body
import soot.Unit
import soot.Value
import soot.jimple.*
import java.util.*
import kotlin.collections.HashSet


class BranchSwitching(val codePositionManager: CodePositionManager) : AbstractInstrumentationTransformer() {

    private val branchTargetStmts = HashSet<String>()

    //val predicates = ArrayList<Pair<Int, Boolean>>()
    val positives = ArrayList<Int>()
    val negatives = ArrayList<Int>()
    val switches  = ArrayList<Int>()

    val executed  = HashSet<Stmt>()

    override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
        // Do not instrument methods in framework classes
        if (!canInstrumentMethod(body.method))
            return

        // var codePos = 0

        loadSwitches()

        val iterator = body.units.snapshotIterator()

        while (iterator.hasNext()) {
            val unit = iterator.next()

            if (unit is IfStmt && !unit.hasTag(InstrumentedCodeTag.name)) {
                val codePos = codePositionManager.getCodePositionForUnit(unit)
                var condition: Value = unit.condition
                if (positives.contains(codePos.id)) {
                    condition = Jimple.v().newEqExpr(IntConstant.v(0), IntConstant.v(0))
                    executed.add(unit.target)
                } else if (negatives.contains(codePos.id)) {
                    condition = Jimple.v().newEqExpr(IntConstant.v(0), IntConstant.v(1))
                    executed.add(getUnitAfter(body, unit))
                } else if (switches.contains(codePos.id)) {
                    when (condition) {
                        is EqExpr -> { condition = Jimple.v().newNeExpr(condition.op1, condition.op2) }
                        is NeExpr -> { condition = Jimple.v().newEqExpr(condition.op1, condition.op2) }
                        is GtExpr -> { condition = Jimple.v().newLeExpr(condition.op1, condition.op2) }
                        is LtExpr -> { condition = Jimple.v().newGeExpr(condition.op1, condition.op2) }
                        is GeExpr -> { condition = Jimple.v().newLtExpr(condition.op1, condition.op2) }
                        is LeExpr -> { condition = Jimple.v().newGtExpr(condition.op1, condition.op2) }
                    }
                    executed.add(unit.target)
                } else {
                    continue
                }

                //predicates.add(counter++, condition)
                switchBranch(unit, condition)
            }
        }

    }

    private fun loadSwitches() {
        positives.add(181009)
        negatives.add(98025)
        switches.add(5030)
    }


    private fun switchBranch(ifStmt: IfStmt, condition: Value) {
        println("[Before switch] " + ifStmt)
        ifStmt.condition = condition
        println("[After  switch] " + ifStmt)
    }

    private fun getUnitBefore(body: Body, unit: Unit): Stmt {
        var u = body.units.first
        while (u != unit)
            u = body.units.getSuccOf(u)

        val unitBefore = body.units.getPredOf(unit)
        return unitBefore as Stmt
    }

    private fun getUnitAfter(body: Body, unit: Unit): Stmt {
        var u = body.units.first
        while (u != unit)
            u = body.units.getSuccOf(u)

        val unitAfter = body.units.getSuccOf(unit)
        return unitAfter as Stmt
    }

}
