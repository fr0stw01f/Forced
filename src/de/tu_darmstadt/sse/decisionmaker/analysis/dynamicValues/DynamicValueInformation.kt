package de.tu_darmstadt.sse.decisionmaker.analysis.dynamicValues

import soot.jimple.Stmt
import de.tu_darmstadt.sse.decisionmaker.analysis.symbolicexecution.datastructure.SMTBinding


class DynamicValueInformation(val statement: Stmt, val binding: SMTBinding) {
    var isBaseObject = false
    var argPos = -1
    //this information is required for the SMT-solver problem with digits as Strings
    var sourceOfDataflow: Stmt? = null
}
