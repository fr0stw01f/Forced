package me.zhenhao.forced.additionalappclasses;

import android.app.Application;
import android.content.Context;
import me.zhenhao.forced.additionalappclasses.tracing.BytecodeLogger;

public class ForcedCodeInitHelperApp extends Application{
			
	protected void attachBaseContext(Context context) {
		super.attachBaseContext(context);
		BytecodeLogger.initialize(context);
	}

}
