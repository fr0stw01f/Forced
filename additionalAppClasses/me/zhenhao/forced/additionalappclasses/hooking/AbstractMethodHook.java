package me.zhenhao.forced.additionalappclasses.hooking;

import me.zhenhao.forced.additionalappclasses.tracing.BytecodeLogger;


public abstract class AbstractMethodHook {
	
	
	protected int getLastCodePosition() {
		return BytecodeLogger.getLastExecutedStatement();
	}
	
	
	public abstract boolean isValueReplacementNecessary();

}
