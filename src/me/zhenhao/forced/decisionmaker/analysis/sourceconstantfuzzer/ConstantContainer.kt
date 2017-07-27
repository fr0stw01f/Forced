package me.zhenhao.forced.decisionmaker.analysis.sourceconstantfuzzer

import java.util.HashMap
import java.util.concurrent.ConcurrentSkipListSet

class ConstantContainer {

    // map class -> constants in a set to avoid duplicates
    private val tmpSetMap = HashMap<String, ConcurrentSkipListSet<Any>>()
    // map class -> constants in array for random access via index
    val arrayMap = HashMap<String, Array<Any>>()

    private val allValuesSet = ConcurrentSkipListSet<Any>()
    var allValues = arrayOf<Any>()
        private set

    var isEmpty = true
        private set


    fun insertTmpValue(className: String, value: Any) {
        val constantSet: ConcurrentSkipListSet<Any>?

        if (tmpSetMap.containsKey(className)) {
            constantSet = tmpSetMap[className]
        } else {
            constantSet = ConcurrentSkipListSet<Any>()
            tmpSetMap.put(className, constantSet)
        }
        constantSet!!.add(value)
        allValuesSet.add(value)

        isEmpty = false
    }


    fun convertSetsToArrays() {
        for (clazz in tmpSetMap.keys) {
            val constantSet = tmpSetMap[clazz]
            val constantObjects = constantSet!!.toTypedArray()

            arrayMap.put(clazz, constantObjects)
        }

        allValues = allValuesSet.toTypedArray()
    }

}
