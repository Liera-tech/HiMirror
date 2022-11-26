package com.liera.hi.linkwifi;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class LinkWifiManager {

    private static final String TAG = "LinkWifiManager";
    private final LinkWifiManagerService mLinkWifiManagerService;
    private final LinkWifiP2PManagerService mLinkWifiP2PManagerService;
    private final ConcurrentHashMap<Class<?>, LinkWifiManagerCallback> callbackMap = new ConcurrentHashMap<>();
    private boolean isRegisterReceiver;

    public LinkWifiManager(Context context) {
        this.mLinkWifiManagerService = new LinkWifiManagerService(context, this);
        this.mLinkWifiP2PManagerService = new LinkWifiP2PManagerService(context, this);
    }

    public void addCallback(Class<?> clazz, LinkWifiManagerCallback linkWifiManagerCallback) {
        LinkWifiManagerCallback callback = callbackMap.get(clazz);
        if (callback != null) return;
        Log.d(TAG, "addCallback clazz = " + clazz);
        callbackMap.put(clazz, linkWifiManagerCallback);
    }

    public void removeCallback(Class<?> clazz) {
        LinkWifiManagerCallback callback = callbackMap.get(clazz);
        if (callback == null) return;
        Log.d(TAG, "removeCallback clazz = " + clazz);
        callbackMap.remove(clazz);
    }

    public void clearCallback() {
        Log.d(TAG, "clearCallback");
        callbackMap.clear();
    }

    public void register() {
        if (isRegisterReceiver) return;
        isRegisterReceiver = true;
        Log.d(TAG, "register");
        mLinkWifiManagerService.register();
        mLinkWifiP2PManagerService.register();
    }

    public void unRegister() {
        if (!isRegisterReceiver) return;
        isRegisterReceiver = false;
        Log.d(TAG, "unRegister");
        mLinkWifiManagerService.unRegister();
        mLinkWifiP2PManagerService.unRegister();
    }

    public void notifyWifiEnable() {
        for (Class<?> key : callbackMap.keySet()) {
            LinkWifiManager.LinkWifiManagerCallback linkWifiManagerCallback = callbackMap.get(key);
            if (linkWifiManagerCallback != null)
                linkWifiManagerCallback.wifiEnable();
        }
    }

    public void notifyWifiDisable() {
        for (Class<?> key : callbackMap.keySet()) {
            LinkWifiManager.LinkWifiManagerCallback linkWifiManagerCallback = callbackMap.get(key);
            if (linkWifiManagerCallback != null)
                linkWifiManagerCallback.wifiDisable();
        }
    }

    public interface LinkWifiManagerCallback {
        void wifiEnable();

        void wifiDisable();
    }
}
