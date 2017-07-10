package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure;


public abstract class SMTAssignment implements SMTStatement{			
	private final SMTBinding lhs;

	public SMTAssignment(SMTBinding lhs) {
		this.lhs = lhs;
	}
	
	public SMTBinding getLhs() {
		return lhs;
	}
}
