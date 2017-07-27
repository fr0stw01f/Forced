package me.zhenhao.forced.decisionmaker.analysis.dynamicValues


class DynamicIntValue(codePosition: Int, paramIdx: Int, val intValue: Int) : DynamicValue(codePosition, paramIdx) {

    override fun toString(): String {
        return intValue.toString() + ""
    }

    override fun hashCode(): Int {
        var code = 31
        code += codePosition
        code += paramIdx
        code += intValue
        return code
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as DynamicIntValue?
        if (other!!.codePosition != codePosition)
            return false
        if (other.paramIdx != paramIdx)
            return false
        if (other.intValue != intValue)
            return false
        return true
    }
}
