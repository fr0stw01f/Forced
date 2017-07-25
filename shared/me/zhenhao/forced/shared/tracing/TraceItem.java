package me.zhenhao.forced.shared.tracing;

import android.os.Parcel;
import android.os.Parcelable;
import me.zhenhao.forced.shared.networkconnection.IClientRequest;


public abstract class TraceItem implements Parcelable, IClientRequest  {

	private static final long serialVersionUID = 5704527703779833243L;
	
	private int lastExecutedStatement;
	private int globalLastExecutedStatement;
	
	protected TraceItem() {
		super();
	}

	protected TraceItem(int lastExecutedStatement) {
		this(lastExecutedStatement, -1);
	}

	protected TraceItem(int lastExecutedStatement, int globalLastExecutedStatement) {
		this.lastExecutedStatement = lastExecutedStatement;
		this.globalLastExecutedStatement = globalLastExecutedStatement;
	}
	
	public int getLastExecutedStatement() {
		return this.lastExecutedStatement;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int arg1) {
		parcel.writeInt(lastExecutedStatement);
		parcel.writeInt(globalLastExecutedStatement);
	}
	
	protected void readFromParcel(Parcel parcel) {
		this.lastExecutedStatement = parcel.readInt();
		this.globalLastExecutedStatement = parcel.readInt();
	}
	
}
