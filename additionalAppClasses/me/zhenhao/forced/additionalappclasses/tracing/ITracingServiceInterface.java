package me.zhenhao.forced.additionalappclasses.tracing;

import me.zhenhao.forced.sharedclasses.tracing.TraceItem;


interface ITracingServiceInterface {

	void dumpQueue();
	
	void enqueueTraceItem(TraceItem ti);
	
}
