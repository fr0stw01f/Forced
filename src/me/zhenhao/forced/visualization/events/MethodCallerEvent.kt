package me.zhenhao.forced.visualization.events

import me.zhenhao.forced.sharedclasses.SharedClassesSettings


class MethodCallerEvent(processId: Int, lastCodePosition: Long, methodSignature: String, private val methodCalleeWithRuntimeValues: String) : AbstractPathExecutionEvent(processId, lastCodePosition, methodSignature) {

    override fun toString(): String {
        return String.format("%s %s -> %s", SharedClassesSettings.METHOD_CALLER_LABEL,
                methodSignature, methodCalleeWithRuntimeValues)
    }


    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += processId
        hashCode += lastCodePosition.toInt()
        hashCode += methodSignature.hashCode()
        hashCode += methodCalleeWithRuntimeValues.hashCode()
        return hashCode
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is MethodCallerEvent)
            return false
        if (obj === this)
            return true

        val rhs = obj
        return this.processId == rhs.processId &&
                this.methodSignature == rhs.methodSignature &&
                this.methodCalleeWithRuntimeValues == rhs.methodCalleeWithRuntimeValues &&
                this.lastCodePosition == rhs.lastCodePosition
    }
}
