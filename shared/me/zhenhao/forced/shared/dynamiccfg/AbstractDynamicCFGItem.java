package me.zhenhao.forced.shared.dynamiccfg;

import me.zhenhao.forced.shared.tracing.TraceItem;


public abstract class AbstractDynamicCFGItem extends TraceItem {

	private static final long serialVersionUID = -5500762826791899632L;

	AbstractDynamicCFGItem() {
		super();
	}
	
	AbstractDynamicCFGItem(int lastExecutedStatement) {
		super(lastExecutedStatement);
	}

}
