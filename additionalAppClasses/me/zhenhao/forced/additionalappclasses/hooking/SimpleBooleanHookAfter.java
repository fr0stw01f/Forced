package me.zhenhao.forced.additionalappclasses.hooking;

import android.util.Log;
import me.zhenhao.forced.sharedclasses.SharedClassesSettings;
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest;
import me.zhenhao.forced.sharedclasses.networkconnection.NetworkConnectionInitiator;
import me.zhenhao.forced.sharedclasses.networkconnection.ServerCommunicator;
import me.zhenhao.forced.sharedclasses.networkconnection.ServerResponse;


public class SimpleBooleanHookAfter extends AbstractMethodHookAfter{
	private final String methodSignature;
	
	private Object runtimeValueOfReturnAfterhooking;
	
	private boolean runtimeValueOfReturnAvailable;
	
	public SimpleBooleanHookAfter(String methodSignature) {
		this.methodSignature = methodSignature;
	}
	
	public void retrieveBooleanValueFromServer() {
		ServerCommunicator sc = NetworkConnectionInitiator.getServerCommunicator();
		int lastCodePosition = getLastCodePosition();
		DecisionRequest cRequest = new DecisionRequest(lastCodePosition, methodSignature, true);
		
		ServerResponse response = sc.getResultForRequest(cRequest);
		
		if (response == null) {
			Log.e(SharedClassesSettings.TAG, "NULL response received from server");
			runtimeValueOfReturnAvailable = false;
			runtimeValueOfReturnAfterhooking = null;
			return;
		}

		Log.i(SharedClassesSettings.TAG, "Retrieved boolean decision from server");
		runtimeValueOfReturnAvailable = response.doesResponseExist();

		if(runtimeValueOfReturnAvailable) {
			runtimeValueOfReturnAfterhooking = response.getReturnValue();
			Log.d(SharedClassesSettings.TAG, "Return value from server: "
					+ runtimeValueOfReturnAfterhooking);
		}
		else
			Log.d(SharedClassesSettings.TAG, "Server had no response value for us");
	}
	
	@Override
	public Object getReturnValue() {
		return runtimeValueOfReturnAfterhooking;
	}

	@Override
	public boolean isValueReplacementNecessary() {
		return runtimeValueOfReturnAvailable;
	}

}
