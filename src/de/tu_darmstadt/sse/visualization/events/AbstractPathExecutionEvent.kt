package de.tu_darmstadt.sse.visualization.events


abstract class AbstractPathExecutionEvent(val processId: Int, val lastCodePosition: Long, val methodSignature: String) {

    abstract override fun toString(): String
    abstract override fun hashCode(): Int
    abstract override fun equals(o: Any?): Boolean

}
