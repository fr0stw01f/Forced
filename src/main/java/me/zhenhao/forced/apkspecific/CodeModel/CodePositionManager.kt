package me.zhenhao.forced.apkspecific.CodeModel

import java.util.HashMap

import soot.Unit


class CodePositionManager private constructor() {

    val methodToLastCodePosition = HashMap<String, Int>()
    val unitToCodePosition = HashMap<Unit, CodePosition>()
    val codePositionToUnit = HashMap<CodePosition, Unit>()
    val idToCodePosition = HashMap<Int, CodePosition>()
    val methodToOffset = HashMap<String, Int>()
    var lastOffset = 1

    fun getCodePositionForUnit(u: Unit): CodePosition {
        return unitToCodePosition[u]!!
    }

    fun getCodePositionForUnit(u: Unit, methodSignature: String, lineNumber: Int, sourceLineNumber: Int): CodePosition {
        var codePos: CodePosition? = unitToCodePosition[u]
        if (codePos == null) {
            val offset = getMethodOffset(methodSignature)
            val lastCodePos = getAndIncrementLastCodePosition(methodSignature)

            codePos = CodePosition(offset + lastCodePos, methodSignature, lineNumber, sourceLineNumber)
            unitToCodePosition.put(u, codePos)
            codePositionToUnit.put(codePos, u)
            idToCodePosition.put(codePos.id, codePos)
        }
        return codePos
    }

    private fun getAndIncrementLastCodePosition(methodSignature: String): Int {
        var lastPos: Int? = this.methodToLastCodePosition[methodSignature]
        if (lastPos == null)
            lastPos = 0
        else
            lastPos++
        this.methodToLastCodePosition.put(methodSignature, lastPos)
        return lastPos
    }

    internal fun getMethodOffset(methodSignature: String): Int {
        return this.methodToOffset.computeIfAbsent(methodSignature) { lastOffset++ * methodOffsetMultiplier }
    }

    internal val methodsWithCodePositions: Set<String>
        get() = methodToOffset.keys

    fun getUnitForCodePosition(cp: CodePosition): Unit {
        return codePositionToUnit[cp]!!
    }

    fun getUnitForCodePosition(id: Int): Unit? {
        val cp = idToCodePosition[id] ?: return null
        return codePositionToUnit[cp]
    }

    fun getCodePositionByID(id: Int): CodePosition {
        return idToCodePosition[id]!!
    }

    companion object {
        private val methodOffsetMultiplier = 1000
        val singleton = CodePositionManager()
    }

}
