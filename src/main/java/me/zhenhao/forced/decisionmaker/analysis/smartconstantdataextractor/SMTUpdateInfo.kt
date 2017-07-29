package me.zhenhao.forced.decisionmaker.analysis.smartconstantdataextractor

import soot.jimple.Stmt
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTAssertStatement


class SMTUpdateInfo(val assertionStmt: SMTAssertStatement, val stmt: Stmt, val sourceOfDataflow: Stmt)
