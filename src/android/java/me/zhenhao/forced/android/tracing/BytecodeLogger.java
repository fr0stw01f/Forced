package me.zhenhao.forced.android.tracing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import me.zhenhao.forced.android.networkconnection.NetworkConnectionInitiator;
import me.zhenhao.forced.android.networkconnection.ServerCommunicator;
import me.zhenhao.forced.shared.SharedClassesSettings;
import me.zhenhao.forced.shared.dynamiccfg.MethodCallItem;
import me.zhenhao.forced.shared.dynamiccfg.MethodEnterItem;
import me.zhenhao.forced.shared.dynamiccfg.MethodLeaveItem;
import me.zhenhao.forced.shared.dynamiccfg.MethodReturnItem;
import me.zhenhao.forced.shared.networkconnection.IClientRequest;
import me.zhenhao.forced.shared.tracing.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class BytecodeLogger {

    public static Context applicationContext;

    private static ThreadLocal<Integer> lastExecutedStatement = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return -1;
        }

    };
    private static int globalLastExecutedStatement;

    private static final Queue<TraceItem> bootupQueue = new LinkedBlockingQueue<>();
    private static ITracingServiceInterface tracingService = null;
    private static ServiceConnection tracingConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            tracingService = null;
            Log.i(SharedClassesSettings.TAG, "Tracing service disconnected");
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder serviceBinder) {
            Log.i(SharedClassesSettings.TAG, "Tracing service connected");
            if (serviceBinder == null) {
                Log.e(SharedClassesSettings.TAG, "Got a null binder. Shitaki.");
                return;
            }

            try {
                tracingService = ((TracingService.TracingServiceBinder) serviceBinder).getService();
            }
            catch (RuntimeException ex) {
                Log.e(SharedClassesSettings.TAG, "Could not get tracing service: " + ex.getMessage());
            }
        }

    };

    BytecodeLogger() {
        // this constructor shall just prevent external code from instantiating this class
    }


    public static void initialize(final Context context) {
        Log.i(SharedClassesSettings.TAG_FORCED, "Initialize bytecode logger with context" + context + "...");
        applicationContext = context;
        NetworkConnectionInitiator.initNetworkConnection();

        // Start the service in its own thread to avoid an ANR
        if (tracingService == null) {
            Thread initThread = new Thread() {

                @Override
                public void run() {
                    if (tracingService == null) {
                        Log.i(SharedClassesSettings.TAG_FORCED, "Binding to tracing service...");
                        Intent serviceIntent = new Intent(context, TracingService.class);
                        serviceIntent.setAction(TracingService.ACTION_NULL);
                        context.startService(serviceIntent);
                        if (context.bindService(serviceIntent, tracingConnection, Context.BIND_AUTO_CREATE))
                            Log.i(SharedClassesSettings.TAG_FORCED, "Tracing service bound.");
                        else
                            Log.i(SharedClassesSettings.TAG_FORCED, "bindService() returned false.");
                    }
                }

            };
            initThread.start();
        }
        Log.i(SharedClassesSettings.TAG_FORCED, "Bytecode logger ready...");
    }


    public static void setLastExecutedStatement(int statementID) {
        lastExecutedStatement.set(statementID);
        globalLastExecutedStatement = statementID;
    }


    public static int getLastExecutedStatement() {
        return lastExecutedStatement.get();
    }


    private static Context getAppContext() {
        if(applicationContext != null)
            return applicationContext;
        try {
            Log.i(SharedClassesSettings.TAG_FORCED, "AppContext is null");
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method method = activityThreadClass.getMethod("currentApplication");
            Context app = (Context) method.invoke(null, (Object[]) null);

            if (app == null) {
                Class<?> appGlobalsClass = Class.forName("android.app.AppGlobals");
                method = appGlobalsClass.getMethod("getInitialApplication");
                app = (Context) method.invoke(null, (Object[]) null);
            }

            return app;
        }
        catch (Exception ex) {
            throw new RuntimeException("Could not get context");
        }
    }

    @SuppressWarnings("unused")
    public static void reportConditionOutcome(boolean decision) {
        reportConditionOutcome(decision, getAppContext());
    }

    @SuppressWarnings("unused")
    private static void reportConditionOutcome(boolean decision, Context context) {
        Intent serviceIntent = new Intent(context, TracingService.class);
        serviceIntent.setAction(TracingService.ACTION_ENQUEUE_TRACE_ITEM);
        serviceIntent.putExtra(TracingService.EXTRA_ITEM_TYPE, TracingService.ITEM_TYPE_PATH_TRACKING);
        serviceIntent.putExtra(TracingService.EXTRA_TRACE_ITEM, (Parcelable)
                new PathTrackingTraceItem(getLastExecutedStatement(), decision));
        context.startService(serviceIntent);
    }

    @SuppressWarnings("unused")
    public static void reportConditionOutcomeSynchronous(boolean decision) {
        reportConditionOutcomeSynchronous(decision, getAppContext());
    }


    private static void reportConditionOutcomeSynchronous(boolean decision, Context context) {
        // Create the trace item to be enqueued
        int lastStmt = getLastExecutedStatement();

        Log.i(SharedClassesSettings.TAG_FORCED, lastStmt + "\t0x" + Integer.toHexString(lastStmt) + "\t" + decision);

        TraceItem traceItem = new PathTrackingTraceItem(lastStmt, decision);
        sendTraceItemSynchronous(context, traceItem);
        dumpTracingDataSynchronous();
    }

    @SuppressWarnings("unused")
    public static void reportConditionSynchronous() {
        reportConditionSynchronous(getAppContext());
    }


    private static void reportConditionSynchronous(Context context) {
        // Create the trace item to be enqueued
        int lastStmt = getLastExecutedStatement();

        Log.i(SharedClassesSettings.TAG_FORCED, lastStmt + "\t0x" + Integer.toHexString(lastStmt));

        TraceItem traceItem = new ConditionTraceItem(lastStmt+1);
        sendTraceItemSynchronous(context, traceItem);
        dumpTracingDataSynchronous();
    }


    private static void flushBootupQueue() {
        if (tracingService == null || bootupQueue.isEmpty())
            return;

        synchronized (bootupQueue) {
            if (bootupQueue.isEmpty())
                return;

            // Flush it
            while (!bootupQueue.isEmpty()) {
                TraceItem ti = bootupQueue.poll();
                if (ti != null)
                    tracingService.enqueueTraceItem(ti);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void dumpTracingData() {
        dumpTracingData(getAppContext());
    }


    private static void dumpTracingData(Context context) {
        Log.i(SharedClassesSettings.TAG, "Sending an intent to dump tracing data...");
        Intent serviceIntent = new Intent(context, TracingService.class);
        serviceIntent.setAction(TracingService.ACTION_DUMP_QUEUE);
        context.startService(serviceIntent);
        Log.i(SharedClassesSettings.TAG, "Tracing data dumped via intent");
    }


    public static void dumpTracingDataSynchronous() {
        dumpTracingDataSynchronous(getAppContext());
    }


    private static void dumpTracingDataSynchronous(Context context) {
        // If we don't have a service connection yet, we must directly send the
        // contents of our boot-up queue
        if (tracingService == null && !bootupQueue.isEmpty()) {
            Log.i(SharedClassesSettings.TAG, String.format("Flushing "
                    + "boot-up queue of %d elements...", bootupQueue.size()));
            ServerCommunicator communicator = new ServerCommunicator(bootupQueue);
            List<IClientRequest> items = new ArrayList<>(bootupQueue.size());
            while (!bootupQueue.isEmpty()) {
                TraceItem ti = bootupQueue.poll();
                if (ti == null)
                    break;
                items.add(ti);
            }
            communicator.send(items, true);
            Log.i(SharedClassesSettings.TAG, "All elements in queue sent.");
            return;
        }
        else {
            // If we have a service connection, we must make sure to flush the
            // trace items we accumulated during boot-up
            flushBootupQueue();
        }

        try {
            Log.i(SharedClassesSettings.TAG, "Dumping trace queue on binder...");
            tracingService.dumpQueue();
            Log.i(SharedClassesSettings.TAG, "Done.");
        }
        catch (RuntimeException ex) {
            Log.e(SharedClassesSettings.TAG, "Binder communication failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public static void reportMethodCallSynchronous(int codePosition) {
        reportMethodCallSynchronous(codePosition, getAppContext());
    }


    private static void reportMethodCallSynchronous(int codePosition, Context context) {
        sendTraceItemSynchronous(context, new MethodCallItem(codePosition));
    }

    @SuppressWarnings("unused")
    public static void reportMethodReturnSynchronous(int codePosition) {
        reportMethodReturnSynchronous(codePosition, getAppContext());
    }


    private static void reportMethodReturnSynchronous(int codePosition, Context context) {
        sendTraceItemSynchronous(context, new MethodReturnItem(codePosition));
    }

    @SuppressWarnings("unused")
    public static void reportMethodEnterSynchronous(int codePosition) {
        reportMethodEnterSynchronous(codePosition, getAppContext());
    }


    private static void reportMethodEnterSynchronous(int codePosition, Context context) {
        sendTraceItemSynchronous(context, new MethodEnterItem(codePosition));
    }

    @SuppressWarnings("unused")
    public static void reportMethodLeaveSynchronous(int codePosition) {
        reportMethodLeaveSynchronous(codePosition, getAppContext());
    }


    private static void reportMethodLeaveSynchronous(int codePosition, Context context) {
        sendTraceItemSynchronous(context, new MethodLeaveItem(codePosition));
    }

    @SuppressWarnings("unused")
    public static void reportTargetReachedSynchronous() {
        reportTargetReachedSynchronous(getAppContext());
    }


    private static void reportTargetReachedSynchronous(Context context) {
        Log.i(SharedClassesSettings.TAG, "Target location has been reached.");

        sendTraceItemSynchronous(context, new TargetReachedTraceItem(getLastExecutedStatement()));

        // This is usually the end of the analysis, so make sure to get our
        // data out
        dumpTracingDataSynchronous(context);
    }

    @SuppressWarnings("unused")
    public static void sendDexFileToServer(String dexFileName, byte[] dexFile) {
        sendDexFileToServer(dexFileName, dexFile, getAppContext());
    }


    private static void sendDexFileToServer(String dexFileName, byte[] dexFile, Context context) {
        // Since dex files can be large and we need to make sure that they are
        // sent even if the app crashes afterwards, we write them to disk. The
        // separate watchdog app will pick them up there.
        TraceItem ti = new DexFileTransferTraceItem(dexFileName, dexFile,
                getLastExecutedStatement(), globalLastExecutedStatement);
        Log.i(SharedClassesSettings.TAG, "Writing dex file of " + dexFile.length
                + " bytes at location " + getLastExecutedStatement()
                + " (" + ti.getLastExecutedStatement() + " in object)");
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            // Create the target directory
            File storageDir = FileBasedTracingUtils.getFuzzerDirectory();

            // Serialize the object
            File targetFile;
            try {
                targetFile = File.createTempFile("evofuzz", ".dat", storageDir);
                fos = new FileOutputStream(targetFile);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(ti);
                Log.i(SharedClassesSettings.TAG, "Dex file written to disk for watchdog");
            } catch (IOException e) {
                // ignore it, we can't really do much about it
                Log.e(SharedClassesSettings.TAG, "Could not write serialized trace item to disk: "
                        + e.getMessage());
            }
        }
        finally {
            if (oos != null)
                try {
                    oos.close();
                } catch (IOException e1) {
                    // ignore it, there's little we can do
                    Log.e(SharedClassesSettings.TAG, "Could not close object stream");
                }
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    // ignore it, there's little we can do
                    Log.e(SharedClassesSettings.TAG, "Could not close file stream");
                }
        }
    }

    @SuppressWarnings("unused")
    public static void reportDynamicValue(String dynamicValue, int paramIdx) {
        reportDynamicValue(getAppContext(), dynamicValue, paramIdx);
    }


    private static void reportDynamicValue(Context context, String dynamicValue,
                                           int paramIdx) {
        if (dynamicValue != null && dynamicValue.length() > 0) {
            sendTraceItemSynchronous(context, new DynamicStringValueTraceItem(
                    dynamicValue, paramIdx, getLastExecutedStatement()));
        }
    }

    @SuppressWarnings("unused")
    public static void reportDynamicValue(int dynamicValue, int paramIdx) {
        reportDynamicValue(getAppContext(), dynamicValue, paramIdx);
    }


    private static void reportDynamicValue(Context context, int dynamicValue,
                                           int paramIdx) {
        sendTraceItemSynchronous(context, new DynamicIntValueTraceItem(
                dynamicValue, paramIdx, getLastExecutedStatement()));
    }

    @SuppressWarnings("unused")
    public static void reportTimingBomb(long originalValue, long newValue) {
        reportTimingBomb(getAppContext(), originalValue, newValue);
    }


    private static void reportTimingBomb(Context context, long originalValue, long newValue) {
        sendTraceItemSynchronous(context, new TimingBombTraceItem(originalValue, newValue));
    }


    private static void sendTraceItemSynchronous(Context context, TraceItem traceItem) {
        Log.i(SharedClassesSettings.TAG_FORCED, "Sending trace item " + traceItem.toString());
        // If we don't have a service connection yet, we use our own boot-up queue
        if (tracingService == null) {
            bootupQueue.add(traceItem);
            return;
        }
        else {
            // If we have a service connection, we must make sure to flush the
            // trace items we accumulated during boot-up
            flushBootupQueue();
        }

        try {
            tracingService.enqueueTraceItem(traceItem);
        }
        catch (RuntimeException ex) {
            Log.e(SharedClassesSettings.TAG, "Binder communication failed: " + ex.getMessage());
        }
    }

}
