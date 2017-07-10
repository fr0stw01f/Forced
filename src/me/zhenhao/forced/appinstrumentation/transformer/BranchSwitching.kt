package me.zhenhao.forced.appinstrumentation.transformer

import soot.Body
import soot.Value
import soot.jimple.IfStmt
import soot.jimple.IntConstant
import soot.jimple.Jimple
import java.util.*


class BranchSwitching : AbstractInstrumentationTransformer() {

    private val branchTargetStmts = HashSet<String>()

    //val predicates = ArrayList<Pair<Int, Boolean>>()
    val positives  = ArrayList<Int>()
    val negatives  = ArrayList<Int>()
    val switches   = ArrayList<Int>()
    var counter    = 0

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
                } else if (negatives.contains(counter)) {
                    condition = Jimple.v().newEqExpr(IntConstant.v(0), IntConstant.v(1))
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

}
