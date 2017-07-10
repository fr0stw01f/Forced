package me.zhenhao.forced.additionalappclasses.hooking;

import com.morgoo.hook.zhook.MethodHook.MethodHookParam;


public interface Condition {
	public boolean isConditionSatisfied(MethodHookParam param);
}
