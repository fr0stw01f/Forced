package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution

import java.util.ArrayList

import soot.Unit


class ControlFlowPath {
    //control flow path
    internal var path: MutableList<Unit> = ArrayList()

    fun appendPath(pathToAppend: ControlFlowPath) {
        this.path.addAll(pathToAppend.getPath())
    }

    fun deepCopy(): ControlFlowPath {
        val copy = ControlFlowPath()
        for (oldUnit in path)
            copy.path.add(oldUnit)
        return copy
    }

    fun containsUnit(unit: Unit): Boolean {
        return path.contains(unit)
    }

    fun addStmt(unit: Unit) {
        this.path.add(unit)
    }

    fun getPath(): List<Unit> {
        return path
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (unit in path)
            sb.append(unit.toString() + "\n")
        return sb.toString()
    }
}
