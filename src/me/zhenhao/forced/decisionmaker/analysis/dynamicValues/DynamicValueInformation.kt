package me.zhenhao.forced.decisionmaker.analysis.dynamicValues

import soot.jimple.Stmt
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBinding


class DynamicValueInformation(val statement: Stmt, val binding: SMTBinding) {
	var isBaseObject = false
	var argPos = -1
	//this information is required for the SMT-solver problem with digits as Strings
	var sourceOfDataflow: Stmt? = null
}
