package me.zhenhao.forced.apkspecific.CodeModel

import java.util.HashMap

import soot.Unit


class CodePositionManager private constructor() {

    private val methodToLastCodePosition = HashMap<String, Int>()
    private val unitToCodePosition = HashMap<Unit, CodePosition>()
    private val codePositionToUnit = HashMap<CodePosition, Unit>()
    private val idToCodePosition = HashMap<Int, CodePosition>()
    private val methodToOffset = HashMap<String, Int>()
    private var lastOffset = 1

    fun getCodePositionForUnit(u: Unit): CodePosition {
        return unitToCodePosition[u]!!
    }

    fun getCodePositionForUnit(u: Unit, methodSignature: String,
                               lineNumber: Int, sourceLineNumber: Int): CodePosition {
        var codePos: CodePosition? = unitToCodePosition[u]
        if (codePos == null) {
            val offset = getMethodOffset(methodSignature)
            val lastCodePos = getAndIncrementLastCodePosition(methodSignature)

            codePos = CodePosition(offset + lastCodePos, methodSignature,
                    lineNumber, sourceLineNumber)
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
        return (this.methodToOffset as MutableMap<String, Int>).computeIfAbsent(methodSignature) { lastOffset++ * methodOffsetMultiplier }
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
        private var codePositionManager: CodePositionManager? = null

        val codePositionManagerInstance: CodePositionManager
            get() {
                if (codePositionManager == null)
                    codePositionManager = CodePositionManager()
                return codePositionManager!!
            }
    }

}
