package me.zhenhao.forced.additionalappclasses.hooking;

import java.util.Set;

import me.zhenhao.forced.sharedclasses.util.Pair;


public abstract class AbstractMethodHookBefore extends AbstractMethodHook {
	
	public AbstractMethodHookBefore() {
	}	

	
	public abstract Set<Pair<Integer, Object>> getParamValuesToReplace();	
}
