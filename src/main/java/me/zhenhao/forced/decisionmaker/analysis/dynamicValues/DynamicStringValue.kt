package me.zhenhao.forced.decisionmaker.analysis.dynamicValues


class DynamicStringValue(codePosition: Int, paramIdx: Int, val stringValue: String) : DynamicValue(codePosition, paramIdx) {

    override fun toString(): String {
        return stringValue
    }

    override fun hashCode(): Int {
        var code = 31
        code += codePosition
        code += paramIdx
        code += stringValue.hashCode()
        return code
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as DynamicStringValue?
        if (other!!.codePosition != codePosition)
            return false
        if (other.paramIdx != paramIdx)
            return false
        if (other.stringValue != stringValue)
            return false
        return true
    }
}
