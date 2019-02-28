package com.unisoc.wcn.smartrepeater.data;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.unisoc.wcn.smartrepeater.profiles.BleProfile;
import com.unisoc.wcn.smartrepeater.profiles.WiFiManagerProfile;
import com.unisoc.wcn.smartrepeater.service.BluetoothLeService;

import java.util.ArrayList;
import java.util.List;

public class BleDevice {

    private static final String TAG = "BleDevice";

    public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    public static final int STATE_SERVICEDISCOVER = BluetoothLeService.STATE_SERVICEDISCOVER;

    public static final int UP_NAME = 0;
    public static final int UP_RSSI = 1;
    public static final int UP_NAME_RSSI = 2;

    private BluetoothDevice device;
    private int rssi;
    private int state = STATE_DISCONNECTED;
    private BluetoothGatt gatt = null;

    private List<BleProfile> mBleProfiles = new ArrayList<BleProfile>();
    private WiFiManagerProfile mWiFiManagerProfile = null;

    public BleDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BleDevice(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
    }

    public boolean isNameEmpty() {
        String name = device.getName();
        if (name == null || name.length() == 0)
            return true;
        return false;
    }

    public String getName() {
        if (isNameEmpty())
            return "unknow device";
        return device.getName();
    }

    public String getAddress() {
        return device.getAddress();
    }

    public int getRssi() {
        return rssi;
    }

    public String getStatus() {
        String res = "unknow state";
        switch (state) {
            case STATE_DISCONNECTED:
                return "disconnected";
            case STATE_CONNECTING:
                return "connecting";
            case STATE_CONNECTED:
                return "connected";
            case STATE_DISCONNECTING:
                return "disconnecting";
            case STATE_SERVICEDISCOVER:
                return "discoveringService";
        }

        return res;
    }

    public boolean equals(BleDevice device) {
        return device.getAddress().equals(device.getAddress());
    }

    public boolean equalsDevice(BluetoothDevice device) {
        return this.device.getAddress().equals(device.getAddress());
    }

    public boolean update(BluetoothDevice device) {
        boolean needUpdate = false;
        if (isNameEmpty()) {
            this.device = device;
            needUpdate = true;
        }
        return needUpdate;
    }

    public boolean update(BluetoothDevice device, int rssi) {
        boolean needUpdate = false;

        if (this.rssi != rssi) {
            this.rssi = rssi;
            needUpdate = true;
        }

        if (isNameEmpty()) {
            this.device = device;
            needUpdate = true;
        }

        return needUpdate;
    }

    public void updateRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setGatt(BluetoothGatt gatt) {
        if (gatt == null) {
            this.gatt.close();
        }
        this.gatt = gatt;
    }

    public BluetoothGatt getGatt() {
        return this.gatt;
    }

    public void profileInit() {
        if (mWiFiManagerProfile == null) {
            mWiFiManagerProfile = new WiFiManagerProfile();
        }
    }

    public void startProfiles() {
        mBleProfiles.clear();
        if (mWiFiManagerProfile == null) {
            mWiFiManagerProfile = new WiFiManagerProfile();
        }

        if (mWiFiManagerProfile.isEnabled()) {
            mWiFiManagerProfile.start(gatt);
            mWiFiManagerProfile.enableWiFiConfigureNotify(gatt, true);
            mBleProfiles.add(mWiFiManagerProfile);
        }
    }

    public void stopProfiles() {
        mBleProfiles.clear();
        if (mWiFiManagerProfile == null) {
            mWiFiManagerProfile = new WiFiManagerProfile();
        }
        if (mWiFiManagerProfile.isEnabled()) {
            mWiFiManagerProfile.stop(gatt);
            mWiFiManagerProfile.enable(false);
            mWiFiManagerProfile = null;
        }
    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                        int newState) {
        Log.d(TAG, "onConnectionStateChange:  " + gatt.getDevice().getAddress() + " " + status + " -> " + newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            profileInit();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            stopProfiles();
        }
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "onServicesDiscovered");
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService.getSelectedDevice() == null) {
            mBluetoothLeService.setSelectedDevice(this);
        }
        List<BluetoothGattService> services = gatt.getServices();
        for (int i = 0; i < services.size(); i++) {
            Log.d(TAG, "Found Service: " + services.get(i).getUuid());
            if (services.get(i).getUuid().equals(WiFiManagerProfile.WIFIMANAGER_SERVICE)) {
                if (mWiFiManagerProfile == null) {
                    mWiFiManagerProfile = new WiFiManagerProfile();
                }
                mWiFiManagerProfile.enable(true);
            }
        }
        startProfiles();
        for (BleProfile profile : mBleProfiles) {
            profile.onServicesDiscovered(gatt, status);
        }
    }

    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicChanged");
        for (BleProfile profile : mBleProfiles) {
            profile.onCharacteristicChanged(gatt, characteristic);
        }
    }

    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicRead");
        for (BleProfile profile : mBleProfiles) {
            profile.onCharacteristicRead(gatt, characteristic, status);
        }
    }

    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicWrite");
        for (BleProfile profile : mBleProfiles) {
            profile.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    public void onDescriptorRead(BluetoothGatt gatt,
                                 BluetoothGattDescriptor descriptor, int status) {
        Log.d(TAG, "onDescriptorRead");
        for (BleProfile profile : mBleProfiles) {
            profile.onDescriptorRead(gatt, descriptor, status);
        }
    }

    public void onDescriptorWrite(BluetoothGatt gatt,
                                  BluetoothGattDescriptor descriptor, int status) {
        Log.i(TAG, "onDescriptorWrite");
        for (BleProfile profile : mBleProfiles) {
            profile.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        Log.i(TAG, "onReadRemoteRssi");
        for (BleProfile profile : mBleProfiles) {
            profile.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Log.i(TAG, "onMtuChangedMTU --> " + mtu);
        for (BleProfile profile : mBleProfiles) {
            profile.onMtuChanged(gatt, mtu, status);
        }
    }

    public boolean wiFiConfigureCharCmdSend(byte[] data) {
        return mWiFiManagerProfile.wiFiConfigureCharCmdSend(data);
    }
}
