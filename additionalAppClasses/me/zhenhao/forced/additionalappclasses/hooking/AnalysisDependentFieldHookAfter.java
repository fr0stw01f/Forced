package me.zhenhao.forced.additionalappclasses.hooking;

import me.zhenhao.forced.additionalappclasses.util.UtilHook;
import me.zhenhao.forced.sharedclasses.networkconnection.DecisionRequest;
import me.zhenhao.forced.sharedclasses.networkconnection.NetworkConnectionInitiator;
import me.zhenhao.forced.sharedclasses.networkconnection.ServerCommunicator;
import me.zhenhao.forced.sharedclasses.networkconnection.ServerResponse;


public class AnalysisDependentFieldHookAfter extends AbstractFieldHookAfter{
	
	private boolean newValueAvailable;
	
	private Object newValue;
	
	private final String fieldSignature;
	
	public AnalysisDependentFieldHookAfter(String fieldSignature) {
		this.fieldSignature = fieldSignature;
	}
	
	
	public void retrieveValueFromServer(Object runtimeValue) {
		ServerCommunicator sc = NetworkConnectionInitiator.getServerCommunicator();
		int lastCodePosition = getLastCodePosition();
		DecisionRequest cRequest = new DecisionRequest(lastCodePosition, fieldSignature, true);
		Object cleanObject = UtilHook.prepareValueForExchange(runtimeValue);
		cRequest.setRuntimeValueOfReturn(cleanObject);
		ServerResponse response = sc.getResultForRequest(cRequest);
		newValueAvailable = response.doesResponseExist();
		if(newValueAvailable)
			newValue = response.getReturnValue();
	}	
	
	@Override
	public boolean isValueReplacementNecessary() {
		return newValueAvailable;
	}

	@Override
	public Object getNewValue() {
		return newValue;
	}

}
