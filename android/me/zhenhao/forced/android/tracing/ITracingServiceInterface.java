package me.zhenhao.forced.android.tracing;

import me.zhenhao.forced.shared.tracing.TraceItem;


interface ITracingServiceInterface {

	void dumpQueue();

	void enqueueTraceItem(TraceItem ti);

}
