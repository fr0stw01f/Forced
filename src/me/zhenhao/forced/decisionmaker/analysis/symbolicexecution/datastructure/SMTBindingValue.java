package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure;

public class SMTBindingValue implements SMTValue{
	private final SMTBinding value;
	
	public SMTBindingValue(SMTBinding value) {
		this.value = value;
	}
	
	public String toString() {
		return value.getBinding();
	}
}
