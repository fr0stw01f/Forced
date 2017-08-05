package me.zhenhao.forced.shared.tracing;

import android.os.Parcel;


public class ConditionTraceItem extends TraceItem {

    private static final long serialVersionUID = -8948293905139569323L;

    public static final Creator<ConditionTraceItem> CREATOR = new Creator<ConditionTraceItem>() {

        @Override
        public ConditionTraceItem createFromParcel(Parcel parcel) {
            ConditionTraceItem ti = new ConditionTraceItem();
            ti.readFromParcel(parcel);
            return ti;
        }

        @Override
        public ConditionTraceItem[] newArray(int size) {
            return new ConditionTraceItem[size];
        }

    };

    private int branchId;

    private ConditionTraceItem() {
        super();
    }

    public ConditionTraceItem(int branchId, int lastExecutedStatement) {
        super(lastExecutedStatement);
        this.branchId = branchId;
    }

    public int getBranchId() {
        return branchId;
    }

    @Override
    public String toString() {
        return "ConditionTraceItem(" + getLastExecutedStatement() + ")";
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        super.writeToParcel(parcel, arg1);
        parcel.writeInt(branchId);
    }

    @Override
    protected void readFromParcel(Parcel parcel) {
        super.readFromParcel(parcel);
        branchId = parcel.readInt();
    }

}
