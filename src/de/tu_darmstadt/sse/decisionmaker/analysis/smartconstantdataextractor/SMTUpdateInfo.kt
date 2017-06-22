package de.tu_darmstadt.sse.decisionmaker.analysis.smartconstantdataextractor

import soot.jimple.Stmt
import de.tu_darmstadt.sse.decisionmaker.analysis.symbolicexecution.datastructure.SMTAssertStatement


class SMTUpdateInfo(val assertionStmt: SMTAssertStatement, val stmt: Stmt, val sourceOfDataflow: Stmt)
