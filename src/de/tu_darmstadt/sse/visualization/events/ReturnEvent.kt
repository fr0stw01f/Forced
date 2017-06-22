package de.tu_darmstadt.sse.visualization.events

import de.tu_darmstadt.sse.sharedclasses.SharedClassesSettings


class ReturnEvent(processId: Int, lastCodePosition: Long, methodSignature: String, val returnValue: String) : AbstractPathExecutionEvent(processId, lastCodePosition, methodSignature) {

    override fun toString(): String {
        return String.format("%s %s return-value: %s", SharedClassesSettings.RETURN_LABEL, methodSignature, returnValue)
    }

    override fun hashCode(): Int {
        var hashCode = 42
        hashCode += processId
        hashCode += lastCodePosition.toInt()
        hashCode += methodSignature.hashCode()
        hashCode += returnValue.hashCode()
        return hashCode
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is ReturnEvent)
            return false
        if (obj === this)
            return true

        val rhs = obj
        return this.processId == rhs.processId &&
                this.methodSignature == rhs.methodSignature &&
                this.lastCodePosition == rhs.lastCodePosition &&
                this.returnValue == rhs.returnValue
    }
}
