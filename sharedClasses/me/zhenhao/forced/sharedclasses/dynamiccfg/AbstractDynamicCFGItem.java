package me.zhenhao.forced.sharedclasses.dynamiccfg;

import me.zhenhao.forced.sharedclasses.tracing.TraceItem;


public abstract class AbstractDynamicCFGItem extends TraceItem {
	
	
	private static final long serialVersionUID = -5500762826791899632L;

	public AbstractDynamicCFGItem() {
		super();
	}
	
	public AbstractDynamicCFGItem(int lastExecutedStatement) {
		super(lastExecutedStatement);
	}

}
