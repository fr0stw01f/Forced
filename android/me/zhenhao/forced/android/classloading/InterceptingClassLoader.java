package me.zhenhao.forced.android.classloading;

import android.util.Log;
import dalvik.system.DexFile;
import me.zhenhao.forced.shared.SharedClassesSettings;


@SuppressWarnings("unused")
public class InterceptingClassLoader {

	public static Class<?> loadClass(DexFile dexFile, String className, ClassLoader classLoader) throws ClassNotFoundException {
		try {
			Log.i(SharedClassesSettings.TAG, "Loading class " + className);
			// Try the default class loader
			return Class.forName(className);
		}
		catch (ClassNotFoundException ex) {
			try {
				// Try the given class loader
				return classLoader.loadClass(className);
			}
			catch (ClassNotFoundException ex2) {
				// We have no other choice than using the original class loading
				Log.w(SharedClassesSettings.TAG, "Could not intercept class loading");
				return dexFile.loadClass(className, classLoader);
			}
		}
	}
}
