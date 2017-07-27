package me.zhenhao.forced.android;

import android.app.Application;
import android.content.Context;
import me.zhenhao.forced.android.tracing.BytecodeLogger;


@SuppressWarnings("unused")
public class ForcedCodeInitHelperApp extends Application{

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        BytecodeLogger.initialize(context);
    }

}
