package me.zhenhao.forced.additionalappclasses.tracing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import me.zhenhao.forced.sharedclasses.SharedClassesSettings;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodCallItem;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodEnterItem;
import me.zhenhao.forced.sharedclasses.dynamiccfg.MethodLeaveItem;
import me.zhenhao.forced.sharedclasses.networkconnection.IClientRequest;
import me.zhenhao.forced.sharedclasses.networkconnection.NetworkConnectionInitiator;
import me.zhenhao.forced.sharedclasses.networkconnection.ServerCommunicator;
import me.zhenhao.forced.sharedclasses.tracing.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
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
				Log.e(SharedClassesSettings.TAG, "Got a null binder");
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
		Log.i(SharedClassesSettings.TAG_FORCED, "Initialize bytecode logger...");
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

	@SuppressWarnings("unused")
	private static void reportConditionOutcome(boolean decision) {
		Intent serviceIntent = new Intent(applicationContext, TracingService.class);
		serviceIntent.setAction(TracingService.ACTION_ENQUEUE_TRACE_ITEM);
		serviceIntent.putExtra(TracingService.EXTRA_ITEM_TYPE, TracingService.ITEM_TYPE_PATH_TRACKING);
		serviceIntent.putExtra(TracingService.EXTRA_TRACE_ITEM, (Parcelable)
				new PathTrackingTraceItem(getLastExecutedStatement(), decision));
		applicationContext.startService(serviceIntent);
	}

	@SuppressWarnings("unused")
	private static void reportConditionOutcomeSynchronous(boolean decision) {
		// Create the trace item to be enqueued
		int lastStmt = getLastExecutedStatement();

		Log.i(SharedClassesSettings.TAG_FORCED, lastStmt + "\t0x" + Integer.toHexString(lastStmt) + "\t" + decision);

		TraceItem traceItem = new PathTrackingTraceItem(lastStmt, decision);
		sendTraceItemSynchronous(traceItem);
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
	private static void dumpTracingData() {
		Log.i(SharedClassesSettings.TAG, "Sending an intent to dump tracing data...");
		Intent serviceIntent = new Intent(applicationContext, TracingService.class);
		serviceIntent.setAction(TracingService.ACTION_DUMP_QUEUE);
		applicationContext.startService(serviceIntent);
		Log.i(SharedClassesSettings.TAG, "Tracing data dumped via intent");
	}
	
	
	public static void dumpTracingDataSynchronous() {
		// If we don't have a service connection yet, we must directly send the
		// contents of our boot-up queue
		if (tracingService == null && !bootupQueue.isEmpty()) {
			Log.i(SharedClassesSettings.TAG, String.format("Flushing boot-up queue of %d elements...",
					bootupQueue.size()));
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

	@SuppressWarnings("unused")
	private static void reportMethodCallSynchronous(int codePosition) {
		sendTraceItemSynchronous(new MethodCallItem(codePosition));
	}

	@SuppressWarnings("unused")
	private static void reportMethodEnterSynchronous(int codePosition) {
		sendTraceItemSynchronous(new MethodEnterItem(codePosition));
	}

	@SuppressWarnings("unused")
	private static void reportMethodLeaveSynchronous(int codePosition) {
		sendTraceItemSynchronous(new MethodLeaveItem(codePosition));
	}

	@SuppressWarnings("unused")
	private static void reportTargetReachedSynchronous() {
		Log.i(SharedClassesSettings.TAG, "Target location has been reached.");
		
		sendTraceItemSynchronous(new TargetReachedTraceItem(getLastExecutedStatement()));
		
		// This is usually the end of the analysis, so make sure to get our
		// data out
		dumpTracingDataSynchronous();
	}
	
	
	public static void sendDexFileToServer(String dexFileName, byte[] dexFile) {
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
	private static void reportDynamicValue(String dynamicValue, int paramIdx) {
		if (dynamicValue != null && dynamicValue.length() > 0) {
			sendTraceItemSynchronous(new DynamicStringValueTraceItem(dynamicValue, paramIdx, getLastExecutedStatement()));
		}
	}

	@SuppressWarnings("unused")
	private static void reportDynamicValue(int dynamicValue, int paramIdx) {
		sendTraceItemSynchronous(new DynamicIntValueTraceItem(
				dynamicValue, paramIdx, getLastExecutedStatement()));
	}

	@SuppressWarnings("unused")
	private static void reportTimingBomb(long originalValue, long newValue) {
		sendTraceItemSynchronous(new TimingBombTraceItem(originalValue, newValue));
	}
	
	
	private static void sendTraceItemSynchronous(TraceItem traceItem) {
		Log.i(SharedClassesSettings.TAG_FORCED, "Sending trace item " + traceItem.toString());
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
