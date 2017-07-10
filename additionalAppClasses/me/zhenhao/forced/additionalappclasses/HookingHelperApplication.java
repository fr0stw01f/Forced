package me.zhenhao.forced.additionalappclasses;

import android.app.Application;
import android.content.Context;
import me.zhenhao.forced.additionalappclasses.hooking.Hooker;

public class HookingHelperApplication extends Application{	
			
	protected void attachBaseContext(Context context) {
		super.attachBaseContext(context);
		Hooker.initializeHooking(context);
	}

}
