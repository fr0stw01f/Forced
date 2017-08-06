package me.zhenhao.forced.shared.tracing;

import android.os.Parcel;
import android.os.Parcelable;


public class PathTraceItem extends TraceItem {

    private static final long serialVersionUID = -8948293905139569335L;

    public static final Parcelable.Creator<PathTraceItem> CREATOR = new Parcelable.Creator<PathTraceItem>() {

        @Override
        public PathTraceItem createFromParcel(Parcel parcel) {
            PathTraceItem ti = new PathTraceItem();
            ti.readFromParcel(parcel);
            return ti;
        }

        @Override
        public PathTraceItem[] newArray(int size) {
            return new PathTraceItem[size];
        }

    };

    private int branchId;
    private boolean lastConditionResult;

    private PathTraceItem() {
        super();
    }

    public PathTraceItem(int branchId, int lastExecutedStatement, boolean lastConditionResult) {
        super(lastExecutedStatement);
        this.branchId = branchId;
        this.lastConditionResult = lastConditionResult;
    }

    @Override
    public String toString() {
        return "PathTraceItem([" + branchId + ", " + getLastExecutedStatement() + "]->" + getLastConditionalResult() + ")";
    }

    public int getBranchId() {
        return branchId;
    }

    public boolean getLastConditionalResult() {
        return this.lastConditionResult;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        super.writeToParcel(parcel, arg1);
        parcel.writeInt(branchId);
        parcel.writeByte((byte) (lastConditionResult ? 0 : 1));
    }

    @Override
    protected void readFromParcel(Parcel parcel) {
        super.readFromParcel(parcel);
        branchId = parcel.readInt();
        lastConditionResult = parcel.readByte() == 1;
    }

}
