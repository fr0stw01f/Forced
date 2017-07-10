package me.zhenhao.forced.additionalappclasses.hooking;

import me.zhenhao.forced.additionalappclasses.tracing.BytecodeLogger;


public abstract class AbstractFieldHookAfter {
	
	protected int getLastCodePosition() {
		return BytecodeLogger.getLastExecutedStatement();
	}
	
	
	public abstract boolean isValueReplacementNecessary();

	
	public abstract Object getNewValue();
}
