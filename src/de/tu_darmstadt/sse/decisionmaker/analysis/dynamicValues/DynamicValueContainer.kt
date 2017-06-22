package de.tu_darmstadt.sse.decisionmaker.analysis.dynamicValues

import java.util.HashSet

import soot.util.ConcurrentHashMultiMap
import soot.util.MultiMap


class DynamicValueContainer : Cloneable {

    private val dynValueHandlers = HashSet<DynamicValueUpdateHandler>()
    internal val values = ConcurrentHashMultiMap<Int, DynamicValue>()

    constructor()

    private constructor(original: DynamicValueContainer) {
        this.dynValueHandlers.addAll(original.dynValueHandlers)
        this.values.putAll(original.values)
    }

    fun add(codePosition: Int, value: DynamicValue) {
        values.put(codePosition, value)
        for (handler in dynValueHandlers)
            handler.onDynamicValueAvailable(value, codePosition)
    }

    fun getValues(): Set<DynamicValue> {
        return values.values()
    }

    fun getValuesAtCodePosition(codePosition: Int): Set<DynamicValue> {
        return values.get(codePosition)
    }

    fun clear() {
        values.clear()
    }

    fun addDynamicValueUpdateHandler(handler: DynamicValueUpdateHandler) {
        dynValueHandlers.add(handler)
    }

    public override fun clone(): DynamicValueContainer {
        return DynamicValueContainer(this)
    }

}
