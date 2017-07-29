package me.zhenhao.forced.android.networkconnection;

public class NetworkConnectionInitiator {

    private static Object syncToken = new Object();
    private static ServerCommunicator sc = null;

    public static void initNetworkConnection() {
        sc = new ServerCommunicator(syncToken);
    }

    public static ServerCommunicator getServerCommunicator() {
        return sc;
    }
}