package me.zhenhao.forced.android.crashreporter;

import android.util.Log;
import me.zhenhao.forced.android.networkconnection.ServerCommunicator;
import me.zhenhao.forced.android.tracing.BytecodeLogger;
import me.zhenhao.forced.shared.SharedClassesSettings;
import me.zhenhao.forced.shared.crashreporter.CrashReportItem;
import me.zhenhao.forced.shared.networkconnection.IClientRequest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;


@SuppressWarnings("unused")
public class CrashReporter {

    private static final UncaughtExceptionHandler uch = new UncaughtExceptionHandler() {

        private ServerCommunicator communicator = new ServerCommunicator(this);

        @Override
        public void uncaughtException(Thread arg0, Throwable arg1) {
            Log.i(SharedClassesSettings.TAG, "Crash reporter started: " + arg1.toString()
                    + " at " + arg1.getStackTrace());
            if (arg1.getCause() != null)
                Log.i(SharedClassesSettings.TAG, "Cause: " + arg1.getCause().getStackTrace().toString());
            if (arg1.getCause().getCause() != null)
                Log.i(SharedClassesSettings.TAG, "Cause 2: " + arg1.getCause().getCause().toString());
            if (arg1.getCause().getCause().getCause() != null)
                Log.i(SharedClassesSettings.TAG, "Cause 3: " + arg1.getCause().getCause().getCause().toString());

            // Make sure that we flush the trace items before we die
            BytecodeLogger.dumpTracingDataSynchronous();

            // Send the crash report
            CrashReportItem ci = new CrashReportItem(arg1.getMessage(), BytecodeLogger.getLastExecutedStatement());
            communicator.send(Collections.singleton(ci), true);

            dumpExceptionToFile(arg1);
        }

    };

    public static void registerExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(uch);
    }

    private static void dumpExceptionToFile(Throwable arg1) {
        FileWriter fw;
        BufferedWriter out;
        try {
            fw = new FileWriter("/sdcard/condition_outcome.txt");
            out = new BufferedWriter(fw);

            out.write("Crash reporter started: " + arg1.toString() + " at " + arg1.getStackTrace());
            if (arg1.getCause() != null)
                out.write("Cause: " + arg1.getCause().getStackTrace().toString());
            if (arg1.getCause().getCause() != null)
                out.write("Cause 2: " + arg1.getCause().getCause().toString());
            if (arg1.getCause().getCause().getCause() != null)
                out.write("Cause 3: " + arg1.getCause().getCause().getCause().toString());

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
