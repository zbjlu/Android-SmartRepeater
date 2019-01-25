package com.unisoc.wcn.smartrepeater.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.unisoc.wcn.smartrepeater.MainActivity;
import com.unisoc.wcn.smartrepeater.data.BleDevice;

import java.util.List;

public class MainService extends Service {
    private static final String TAG = "MainService";
    private static MainService sInstance = null;
    private static Context mContext = null;

    private BluetoothLeService mBluetoothLeService = null;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        sInstance = this;
        init();
    }

    public static MainService getInstance() {
        if (sInstance == null) {
            startMainService();
        }
        return sInstance;
    }

    public static void startMainService() {
        Log.i(TAG, "startMainService()");
        if (mContext == null) {
            mContext = MainActivity.getInstance().getApplicationContext();
        }
        Intent startServiceIntent = new Intent(mContext, MainService.class);
        mContext.startService(startServiceIntent);
    }

    public static void stopMainService() {
        Log.i(TAG, "stopMainService()");
        try {
            Intent stopServiceIntent = new Intent(mContext, MainService.class);
            mContext.stopService(stopServiceIntent);
        } catch (Exception e) {
            Log.e(TAG, "stopMainService() failed " + e);
        }
    }

    private void init() {
        startServices();
    }

    public void startServices() {
        if (mBluetoothLeService == null) {
            mBluetoothLeService = new BluetoothLeService();
            mBluetoothLeService.startService(mContext);
        }
    }

    private void shutdownServices() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.stopService();
            mBluetoothLeService = null;
        }
    }

    public int connect(BleDevice device) {
        return mBluetoothLeService.connect(device);
    }

    public int disconnect(BleDevice device) {
        return mBluetoothLeService.disconnect(device);
    }

    public List<BleDevice> getCachedDevice() {
        return mBluetoothLeService.getCachedDevice();
    }

    public BluetoothLeService getBluetoothLeService() {
        return mBluetoothLeService;
    }

    public void setSelectedDevice(BleDevice selectedDevice) {
        mBluetoothLeService.setSelectedDevice(selectedDevice);
    }

    public BleDevice getSelectedDevice() {
        return mBluetoothLeService.getSelectedDevice();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        shutdownServices();
        sInstance = null;
    }
}
