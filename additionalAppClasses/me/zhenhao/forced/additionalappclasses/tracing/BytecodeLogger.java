package me.zhenhao.forced.additionalappclasses.tracing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import me.zhenhao.forced.additionalappclasses.hooking.Hooker;
import me.zhenhao.forced.sharedclasses.SharedClassesSettings;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodCallItem;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodEnterItem;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodLeaveItem;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodReturnItem;
import me.zhenhao.forced.sharedclasses.networkconnection.IClientRequest;
import me.zhenhao.forced.sharedclasses.networkconnection.ServerCommunicator;
import me.zhenhao.forced.sharedclasses.tracing.*;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class BytecodeLogger {

	//private static final Map<Integer, Boolean> branchTracking = new TreeMap<>();
	//private static final List<Pair<Integer, Boolean>> branchTracking = new ArrayList<>();
	private static final Set<Integer> executedStatements = new HashSet<>();
	//private static int fileCounter = 0;
	
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
		// this constructor shall just prevent external code from instantiating
		// this class
	}
	
	
	public static void initialize(final Context context) {
		// Start the service in its own thread to avoid an ANR
		if (tracingService == null) {
			Thread initThread = new Thread() {

				@Override
				public void run() {
					if (tracingService == null) {
						Log.i(SharedClassesSettings.TAG, "Binding to tracing service...");
						Intent serviceIntent = new Intent(context, TracingService.class);
						serviceIntent.setAction(TracingService.ACTION_NULL);
						context.startService(serviceIntent);
						if (context.bindService(serviceIntent, tracingConnection, Context.BIND_AUTO_CREATE))
							Log.i(SharedClassesSettings.TAG, "Tracing service bound.");
						else
							Log.i(SharedClassesSettings.TAG, "bindService() returned false.");
					}
				}

			};
			initThread.start();
		}
	}
	
	private static ThreadLocal<Integer> lastExecutedStatement = new ThreadLocal<Integer>() {

		@Override
		protected Integer initialValue() {
			return -1;
		}

	};
	
	
	private static int globalLastExecutedStatement;
		
	
	public static void setLastExecutedStatement(int statementID) {
		lastExecutedStatement.set(statementID);
		globalLastExecutedStatement = statementID;
	}
	
	
	public static int getLastExecutedStatement() {
		int last = lastExecutedStatement.get();
		executedStatements.add(last);
		return last;
	}
	
	
	private static Context getAppContext() {
		if(Hooker.applicationContext != null)
			return Hooker.applicationContext;
		try {
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
	
	
	public static void reportConditionOutcome(boolean decision) {
		reportConditionOutcome(decision, getAppContext());
	}


	private static void reportConditionOutcome(boolean decision, Context context) {
		Intent serviceIntent = new Intent(context, TracingService.class);
		serviceIntent.setAction(TracingService.ACTION_ENQUEUE_TRACE_ITEM);
		serviceIntent.putExtra(TracingService.EXTRA_ITEM_TYPE, TracingService.ITEM_TYPE_PATH_TRACKING);
		serviceIntent.putExtra(TracingService.EXTRA_TRACE_ITEM, (Parcelable)
				new PathTrackingTraceItem(getLastExecutedStatement(), decision));
		context.startService(serviceIntent);
	}

	
	public static void reportConditionOutcomeSynchronous(boolean decision) {
		reportConditionOutcomeSynchronous(decision, getAppContext());
	}
	
	
	private static void reportConditionOutcomeSynchronous(boolean decision, Context context) {
		// Create the trace item to be enqueued
		int lastStmt = getLastExecutedStatement();

		Log.i("BranchTracking", lastStmt + "\t0x" + Integer.toHexString(lastStmt) + "\t" + decision);
		//branchTracking.add(new Pair<>(lastStmt, decision));
		dumpConditionOutcomeToFile(lastStmt, decision);

		TraceItem traceItem = new PathTrackingTraceItem(lastStmt, decision);
		sendTraceItemSynchronous(context, traceItem);
	}

	private static void dumpConditionOutcomeToFile(int lastStmt, boolean decision) {
		String fileCounter = "";
		try {
			FileReader fr = new FileReader(SharedClassesSettings.BRANCH_TRACKING_DIR_PATH + "file_counter.txt");
			BufferedReader br = new BufferedReader(fr);
			fileCounter = br.readLine();

			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String fileName = SharedClassesSettings.BRANCH_TRACKING_DIR_PATH + "bt_"
				+ String.format ("%06d", Integer.valueOf(fileCounter)) + ".txt";
		try {
			FileWriter fw = new FileWriter(fileName, true);
			BufferedWriter bw = new BufferedWriter(fw);

			bw.write(lastStmt + "\t0x" + Integer.toHexString(lastStmt) + "\t" + decision + "\n");

			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			Log.e(SharedClassesSettings.TAG, "Binder communication failed: "
					+ ex.getMessage());
		}
	}

	
	public static void reportMethodCallSynchronous(int codePosition) {
		reportMethodCallSynchronous(codePosition, getAppContext());
	}
	
	
	private static void reportMethodCallSynchronous(int codePosition, Context context) {
		sendTraceItemSynchronous(context, new MethodCallItem(codePosition));
	}
	
	
	public static void reportMethodReturnSynchronous(int codePosition) {
		reportMethodReturnSynchronous(codePosition, getAppContext());
	}
	
	
	private static void reportMethodReturnSynchronous(int codePosition, Context context) {
		sendTraceItemSynchronous(context, new MethodReturnItem(codePosition));
	}
	
	
	public static void reportMethodEnterSynchronous(int codePosition) {
		reportMethodEnterSynchronous(codePosition, getAppContext());
	}
	
	
	private static void reportMethodEnterSynchronous(int codePosition, Context context) {
		sendTraceItemSynchronous(context, new MethodEnterItem(codePosition));
	}
	
	
	public static void reportMethodLeaveSynchronous(int codePosition) {
		reportMethodLeaveSynchronous(codePosition, getAppContext());
	}
	
	
	private static void reportMethodLeaveSynchronous(int codePosition, Context context) {
		sendTraceItemSynchronous(context, new MethodLeaveItem(codePosition));
	}
	
	
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
	
	
	public static void reportDynamicValue(int dynamicValue, int paramIdx) {
		reportDynamicValue(getAppContext(), dynamicValue, paramIdx);
	}
	
	
	private static void reportDynamicValue(Context context, int dynamicValue,
										   int paramIdx) {
		sendTraceItemSynchronous(context, new DynamicIntValueTraceItem(
				dynamicValue, paramIdx, getLastExecutedStatement()));
	}

	
	public static void reportTimingBomb(long originalValue, long newValue) {
		reportTimingBomb(getAppContext(), originalValue, newValue);
	}
	
	
	private static void reportTimingBomb(Context context, long originalValue, long newValue) {
		sendTraceItemSynchronous(context, new TimingBombTraceItem(originalValue, newValue));		
	}
	
	
	private static void sendTraceItemSynchronous(Context context, TraceItem traceItem) {
		// If we don't have a service connection yet, we use our own boot-up
		// queue
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
