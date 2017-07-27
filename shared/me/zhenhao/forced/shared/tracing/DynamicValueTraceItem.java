package me.zhenhao.forced.shared.tracing;

import android.os.Parcel;


public abstract class DynamicValueTraceItem extends TraceItem {

    private static final long serialVersionUID = -8275782123844910280L;

    private int paramIdx;

    DynamicValueTraceItem() {
        super();
    }

    DynamicValueTraceItem(int paramIdx, int lastExecutedStatement) {
        super(lastExecutedStatement);
        this.paramIdx = paramIdx;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        super.writeToParcel(parcel, arg1);
        parcel.writeInt(paramIdx);
    }

    @Override
    protected void readFromParcel(Parcel parcel) {
        super.readFromParcel(parcel);
        this.paramIdx = parcel.readInt();
    }

    public int getParamIdx() {
        return this.paramIdx;
    }

}
