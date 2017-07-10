package me.zhenhao.forced.additionalappclasses.hookdefinitions;

import java.util.HashSet;
import java.util.Set;

import me.zhenhao.forced.additionalappclasses.hooking.HookInfo;
import me.zhenhao.forced.additionalappclasses.hooking.MethodHookInfo;

public class DexFileExtractorHookDefinitions implements Hook{

	@Override
	public Set<HookInfo> initializeHooks() {
		Set<HookInfo> dexFileHooks = new HashSet<HookInfo>();

		MethodHookInfo loadDex = new MethodHookInfo("<dalvik.system.DexFile: dalvik.system.DexFile loadDex(java.lang.String, java.lang.String, int)>");
        loadDex.dexFileExtractorHookBefore(0);
        dexFileHooks.add(loadDex);
        
        return dexFileHooks;
	}

}
