package me.zhenhao.forced.additionalappclasses.hookdefinitions;

import java.util.HashSet;
import java.util.Set;

import me.zhenhao.forced.additionalappclasses.hooking.HookInfo;
import me.zhenhao.forced.additionalappclasses.hooking.MethodHookInfo;

public class SimpleBooleanReturnDefinitions implements Hook{

	@Override
	public Set<HookInfo> initializeHooks() {
		Set<HookInfo> booleanHooks = new HashSet<HookInfo>();

		MethodHookInfo getBooleanSP = new MethodHookInfo("<android.app.SharedPreferencesImpl: boolean getBoolean(java.lang.String, boolean)>");
		getBooleanSP.simpleBooleanHookAfter();
        booleanHooks.add(getBooleanSP);
        
        return booleanHooks;
	}

}
