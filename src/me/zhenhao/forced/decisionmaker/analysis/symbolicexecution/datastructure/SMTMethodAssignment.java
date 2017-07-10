package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure;


public class SMTMethodAssignment extends SMTAssignment{
	private final SMTMethodCall rhs;
	
	public SMTMethodAssignment(SMTBinding lhs, SMTMethodCall rhs) {
		super(lhs);
		this.rhs = rhs;
	}
	
	@Override
	public SMTStatement getStatement() {
		return this;
	}
	
	public String toString() {
		return String.format("(= %s %s)", getLhs().getBinding(), rhs);
	}

}
