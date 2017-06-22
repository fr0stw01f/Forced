package de.tu_darmstadt.sse.visualization.events

import de.tu_darmstadt.sse.sharedclasses.SharedClassesSettings


class MethodCalleeEvent(processId: Int, lastCodePosition: Long, methodSignature: String) : AbstractPathExecutionEvent(processId, lastCodePosition, methodSignature) {

    override fun toString(): String {
        return String.format("%s %s", SharedClassesSettings.METHOD_CALLEE_LABEL, methodSignature)
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += processId
        hashCode += methodSignature.hashCode()
        hashCode += lastCodePosition.toInt()
        return hashCode
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is MethodCalleeEvent)
            return false
        if (obj === this)
            return true

        val rhs = obj
        return this.processId == rhs.processId &&
                this.methodSignature == rhs.methodSignature &&
                this.lastCodePosition == rhs.lastCodePosition
    }
}
