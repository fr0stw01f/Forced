package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution

import java.util.ArrayList


class FragmentedControlFlowPath {
    internal var fragmentedControlFlowPath: MutableList<ControlFlowPath> = ArrayList()

    constructor() {
        //do nothing
    }

    constructor(fragmentedControlFlowPath: MutableList<ControlFlowPath>) {
        this.fragmentedControlFlowPath = fragmentedControlFlowPath
    }

    fun addNewControlFlowGraphFragment(cfp: ControlFlowPath) {
        fragmentedControlFlowPath.add(cfp)
    }

    fun deepCopy(): FragmentedControlFlowPath {
        val newFragmentedControlFlowPath = ArrayList<ControlFlowPath>()
        for (cfp in fragmentedControlFlowPath)
            newFragmentedControlFlowPath.add(cfp.deepCopy())
        return FragmentedControlFlowPath(newFragmentedControlFlowPath)
    }

    override fun toString(): String {
        val sb = StringBuilder()

        for (cfp in fragmentedControlFlowPath) {
            sb.append(cfp.toString() + "\t\n")
        }

        return sb.toString()
    }
}
