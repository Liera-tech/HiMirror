package com.liera.hi.linkwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class LinkWifiManagerService implements ILinkWifiManagerService {

    private static final String TAG = "WifiManagerService";
    private final Context mContext;
    private final NetworkConnectChangedReceiver mConnectChangedReceiver;
    private final LinkWifiManager mLinkWifiManager;

    public LinkWifiManagerService(Context context, LinkWifiManager linkWifiManager) {
        this.mContext = context;
        this.mLinkWifiManager = linkWifiManager;
        this.mConnectChangedReceiver = new NetworkConnectChangedReceiver();
    }

    @Override
    public void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mConnectChangedReceiver, filter);
    }

    @Override
    public void unRegister() {
        mContext.unregisterReceiver(mConnectChangedReceiver);
    }

    public class NetworkConnectChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                //wifi状态改变
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    wifiStateChanged(intent);
                    break;
                //广播是WifiManager.WIFI_STATE_ENABLED状态的同时也会接到这个广播，当然刚打开wifi肯定还没有连接到有效的无线
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    networkStateChanged(intent);
                    break;
            }
        }
    }

    private static void networkStateChanged(Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (null != networkInfo) {
            NetworkInfo.State state = networkInfo.getState();
            //确定是否连接上
            boolean isConnected = state == NetworkInfo.State.CONNECTED;
//            Log.e("H3c", "isConnected" + isConnected);
            if (isConnected) {
                Log.d(TAG, "networkStateChanged Connected");
            } else {
                Log.d(TAG, "networkStateChanged not Connected");
            }
        }
    }

    private void wifiStateChanged(Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                Log.d(TAG, "wifiStateChanged wifi state enabled");
                mLinkWifiManager.notifyWifiEnable();
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                Log.d(TAG, "wifiStateChanged wifi state disabled");
                mLinkWifiManager.notifyWifiDisable();
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                break;
        }
    }
}
