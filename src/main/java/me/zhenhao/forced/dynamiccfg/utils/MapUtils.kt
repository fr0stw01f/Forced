package me.zhenhao.forced.dynamiccfg.utils

import java.util.*


object MapUtils {

    fun <K, V : Comparable<V>> sortByValue(map: Map<K, V>): Map<K, V> {
        val list = LinkedList<Map.Entry<K, V>>(map.entries)
        Collections.sort<Map.Entry<K, V>>(list) { o1, o2 -> -o1.value.compareTo(o2.value) }

        val result = LinkedHashMap<K, V>()
        for ((key, value) in list) {
            result.put(key, value)
        }
        return result
    }

}
