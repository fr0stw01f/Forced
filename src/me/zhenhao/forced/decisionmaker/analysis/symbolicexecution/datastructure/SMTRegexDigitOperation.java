package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure;

public class SMTRegexDigitOperation implements SMTStatement{
	private final SMTBinding stringValue;
	
	public SMTRegexDigitOperation(SMTBinding stringValue) {
		this.stringValue = stringValue;
	}
	
	@Override
	public SMTStatement getStatement() {
		return this;
	}
	
	public String toString() {
		return String.format("( RegexIn %s ( RegexStar ( RegexDigit \"\" ) ) )", stringValue.getBinding());
	}

}
