package me.zhenhao.forced.decisionmaker.analysis.dynamicValues


interface DynamicValueUpdateHandler {
	fun onDynamicValueAvailable(stringValue: DynamicValue, lastExecutedStatement: Int)
}
