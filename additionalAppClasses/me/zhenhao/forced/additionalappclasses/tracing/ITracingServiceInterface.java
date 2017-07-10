package me.zhenhao.forced.additionalappclasses.tracing;

import me.zhenhao.forced.sharedclasses.tracing.TraceItem;


interface ITracingServiceInterface {
	
	
	public void dumpQueue();
	
	
	public void enqueueTraceItem(TraceItem ti);
	
}
