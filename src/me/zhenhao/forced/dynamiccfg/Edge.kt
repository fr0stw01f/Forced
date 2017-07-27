package me.zhenhao.forced.dynamiccfg

import soot.SootMethod
import soot.Unit


class Edge(val callSite: Unit?, val callee: SootMethod?) {

    override fun toString(): String {
        return this.callSite.toString() + " -> " + this.callee
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (callSite?.hashCode() ?: 0)
        result = prime * result + (callee?.hashCode() ?: 0)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as Edge?
        if (callSite == null) {
            if (other!!.callSite != null)
                return false
        } else if (callSite != other!!.callSite)
            return false
        if (callee == null) {
            if (other.callee != null)
                return false
        } else if (callee != other.callee)
            return false
        return true
    }

}
