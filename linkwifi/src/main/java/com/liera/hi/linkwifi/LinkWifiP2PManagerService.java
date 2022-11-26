package com.liera.hi.linkwifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.net.InetAddress;

public class LinkWifiP2PManagerService implements ILinkWifiManagerService, WifiP2pManager.ChannelListener, WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "WifiP2PManagerService";
    private final Context mContext;
    private final WifiP2pManager mWifiP2pManager;
    private final WifiP2pChangedReceiver mWifiP2pChangedReceiver;
    private WifiP2pManager.Channel mChannel;
    private boolean initialized;

    public LinkWifiP2PManagerService(Context context, LinkWifiManager linkWifiManager) {
        this.mContext = context;
        this.mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        this.mWifiP2pChangedReceiver = new WifiP2pChangedReceiver();
    }

    @Override
    public void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mContext.registerReceiver(mWifiP2pChangedReceiver, intentFilter);
    }

    @Override
    public void unRegister() {
        mContext.unregisterReceiver(mWifiP2pChangedReceiver);
    }

    private final class WifiP2pChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                //p2p状态改变
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                    wifiP2pStateChanged(intent);
                    break;
                //发现周围设备时的广播，一般在接到次广播时可以更新设备列表，与蓝牙不同，这里的API是以列表的形式将所有搜索到的设备都返回
                //这个广播不仅在周围设备增减时发送，而且在周围设备和本机设备的连接状态发送变化时，也会发出
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                    wifiP2pPeersChanged(intent);
                    break;
                //设备连接改变时调用
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                    wifiP2pConnectionChanged(intent);
                    break;
                //与当前设备的改变有关，一般注册这个广播后，就会收到，以此来获取当前设备的信息
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                    wifiP2pThisDeviceChanged(intent);
                    break;
                //搜索状态有关的广播，开始搜索和结束搜索时会收到
                case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                    wifiP2pDiscoveryChanged(intent);
                    break;
            }
        }
    }

    private void wifiP2pDiscoveryChanged(Intent intent) {
        int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
        switch (discoveryState) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                Log.d(TAG, "wifiP2pDiscoveryChanged p2p discovery started");
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                Log.d(TAG, "wifiP2pDiscoveryChanged p2p discovery stopped");
//                startSearchP2pDevice();
                break;
        }
    }

    private void wifiP2pThisDeviceChanged(Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        String deviceName = device.deviceName;
        String deviceAddress = device.deviceAddress;
        Log.d(TAG, "wifiP2pThisDeviceChanged deviceName = " + deviceName + " deviceAddress = " + deviceAddress);
    }

    private void wifiP2pConnectionChanged(Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if (networkInfo.isConnected()) {
            Log.d(TAG, "device connected");
            mWifiP2pManager.requestConnectionInfo(mChannel, this);
        } else {
            Log.d(TAG, "device disConnected");
        }

        //保存着一些连接的信息，如groupFormed字段保存是否有组建立，groupOwnerAddress字段保存GO设备的地址信息
        // isGroupOwner字段判断自己是否是GO设备。WifiP2pInfo也可以随时用过wifiP2pManager.requestConnectionInfo来获取
        WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        //存放着当前组成员的信息，这个信息只有GO设备可以获取。同样这个信息也可以通过wifiP2pManager.requestGroupInfo获取
        WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
    }

    private void wifiP2pPeersChanged(Intent intent) {
        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        Log.d(TAG, "wifiP2pPeersChanged device = " + device);
    }

    private void wifiP2pStateChanged(Intent intent) {
        int p2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED);
        switch (p2pState) {
            //P2P可用
            case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                this.mChannel = mWifiP2pManager.initialize(mContext, Looper.getMainLooper(), this);
                initialized = true;
                Log.d(TAG, "wifiP2pStateChanged state enabled");
                createGo();
                break;
            //P2P不可用
            case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                Log.d(TAG, "wifiP2pStateChanged state disabled begin");
                if(!initialized) return;
                initialized = false;
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "wifiP2pStateChanged state disabled 位置权限不可用");
                    return;
                }
                Log.d(TAG, "wifiP2pStateChanged state disabled requestGroupInfo");
                mWifiP2pManager.requestGroupInfo(mChannel, wifiP2pGroup -> {
                    if (wifiP2pGroup == null) {
                        Log.d(TAG, "wifiP2pStateChanged state disabled not group, return");
                    } else {
                        boolean groupOwner = wifiP2pGroup.isGroupOwner();
                        Log.d(TAG, "wifiP2pStateChanged state disabled groupOwner = " + groupOwner);
                        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup success");
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.d(TAG, "removeGroup onFailure");
                            }
                        });
                    }
                });

                Log.d(TAG, "wifiP2pDisabled");
                if (mChannel != null)
                    mChannel.close();
                mChannel = null;
                break;
        }
    }

    @Override
    public void onChannelDisconnected() {
        Log.d(TAG, "onChannelDisconnected");
        mChannel = null;
    }

    //对应wifiP2pConnectionChanged
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        //是否在组中
        boolean groupFormed = wifiP2pInfo.groupFormed;
        //是否是Go
        boolean isGroupOwner = wifiP2pInfo.isGroupOwner;
        //获取群主ip
        InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

        Log.d(TAG, "onConnectionInfoAvailable groupFormed = " + groupFormed + " isGroupOwner = " + isGroupOwner + " groupOwnerAddress = " + groupOwnerAddress);
    }

    private Boolean isDiscoverPeers = false;

    //开始搜索
    public void startSearchP2pDevice() {
        Log.d(TAG, "startSearchP2pDevice");
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "没有地理位置访问权限");
            return;
        }
        if(isDiscoverPeers == null || isDiscoverPeers) return;
        isDiscoverPeers = null;
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isDiscoverPeers = true;
                Log.i(TAG, "start discoverPeers onSuccess");
            }

            @Override
            public void onFailure(int i) {
                isDiscoverPeers = false;
                Log.i(TAG, "start discoverPeers onFailure");
            }
        });
    }

    public void createGo() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "创建Go没有位置权限");
            return;
        }
        Log.d(TAG, "createGo");
        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "createGroup onSuccess");
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "createGroup onFailure");
            }
        });
    }

    //停止搜索
    public void stopSearchP2pDevice(){
        Log.d(TAG, "stopSearchP2pDevice");
        if(isDiscoverPeers == null || !isDiscoverPeers) return;
        isDiscoverPeers = false;
        mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "stop stopPeerDiscovery onSuccess");
            }

            @Override
            public void onFailure(int i) {
                Log.i(TAG, "stop stopPeerDiscovery onFailure");
            }
        });
    }
}
