package me.zhenhao.forced.additionalappclasses.hooking;

import java.util.Set;

import me.zhenhao.forced.sharedclasses.util.Pair;


public class PersistentMethodHookBefore extends AbstractMethodHookBefore {
	
	private final Set<Pair<Integer, Object>> paramValuePair;
	
	public PersistentMethodHookBefore(Set<Pair<Integer, Object>> paramValuePair) {
		this.paramValuePair = paramValuePair;
	}	
	
	@Override
	public Set<Pair<Integer, Object>> getParamValuesToReplace() {
		return paramValuePair;
	}

	
	@Override
	public boolean isValueReplacementNecessary() {
		return true;
	}
	
}
