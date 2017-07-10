package me.zhenhao.forced.additionalappclasses.hookdefinitions;

import java.util.Set;

import me.zhenhao.forced.additionalappclasses.hooking.HookInfo;

public interface Hook {
	public Set<HookInfo> initializeHooks();
}
