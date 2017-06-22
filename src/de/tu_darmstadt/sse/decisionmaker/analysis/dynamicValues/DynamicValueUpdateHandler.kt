package de.tu_darmstadt.sse.decisionmaker.analysis.dynamicValues


interface DynamicValueUpdateHandler {
    fun onDynamicValueAvailable(stringValue: DynamicValue, lastExecutedStatement: Int)
}
