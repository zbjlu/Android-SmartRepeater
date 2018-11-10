package com.unisoc.wcn.smartrepeater.profiles;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.unisoc.wcn.smartrepeater.service.BluetoothLeService;

import java.util.UUID;

/**
 * Created by clay.zhu on 2018/7/31.
 */

public class WiFiManagerProfile implements BleProfile {

    private static final String TAG = "WiFiManagerProfile";

    public final static UUID WIFIMANAGER_SERVICE = UUID.fromString("0000ffa5-0000-1000-8000-00805f9b34fb");
    public final static UUID WIFI_CONFIGURE_CHARACTERISTIC = UUID.fromString("0000ffa6-0000-1000-8000-00805f9b34fb");

    private final static UUID CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean profileState = false;

    private BluetoothGattService mWiFiManagerService = null;
    private BluetoothGattCharacteristic mWiFiConfigureChar = null;
    private BluetoothGattDescriptor mCharactersticConfig = null;

    private boolean ifbusy = false;
    private byte[] waitSendData = null;

    private final int profile = WIFIMANAGER;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                        int newState) {
        Log.d(TAG, "onConnectionStateChange");
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "onServicesDiscovered");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicChanged");
        if (characteristic.getUuid().equals(WIFI_CONFIGURE_CHARACTERISTIC)) {
            BluetoothDevice device = gatt.getDevice();
            byte[] notifyData = characteristic.getValue();
            BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
            mBluetoothLeService.wifiManagerNotifyMessUpdate(device, notifyData);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicRead");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicWrite");
        if (characteristic.getUuid().equals(WIFI_CONFIGURE_CHARACTERISTIC)) {
            if (ifbusy) {
                if (waitSendData != null) {
                    wiFiConfigureCharWaitCmdSend(waitSendData);
                }
                ifbusy = false;
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt,
                                 BluetoothGattDescriptor descriptor, int status) {
        Log.i(TAG, "onDescriptorRead");
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt,
                                  BluetoothGattDescriptor descriptor, int status) {
        Log.i(TAG, "onDescriptorWrite");
        if (descriptor.getCharacteristic().getUuid().equals(WIFI_CONFIGURE_CHARACTERISTIC)) {
            if (descriptor.getUuid().equals(CHARACTERISTIC_CONFIGURATION)) {
                Log.d(TAG, "set mtu --> 150");
                BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
                mBluetoothLeService.requestMtu(gatt, 150);
            }
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt,
                                 int rssi, int status) {
        Log.i(TAG, "onReadRemoteRssi");
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt,
                             int mtu, int status) {
        Log.i(TAG, "onMtuChanged");
    }

    public boolean isEnabled() {
        return profileState;
    }

    @Override
    public void enable(boolean enable) {
        profileState = enable;
    }

    @Override
    public int getProfile() {
        return profile;
    }

    @Override
    public boolean start(BluetoothGatt gatt) {
        if (!profileState) {
            return false;
        }
        try {
            mWiFiManagerService = gatt.getService(WIFIMANAGER_SERVICE);
            Log.e(TAG, "start --> " + mWiFiManagerService);
            if (mWiFiManagerService != null) {
                mWiFiConfigureChar = mWiFiManagerService.getCharacteristic(WIFI_CONFIGURE_CHARACTERISTIC);
            } else {
                Log.d(TAG, "Service characteristic not found for UUID: " + WIFIMANAGER_SERVICE);
            }
        } catch (Exception e) {
            Log.d(TAG, "start ComCmd Profile Failed --> " + e);
        }
        ifbusy = false;
        return false;
    }

    @Override
    public void stop(BluetoothGatt gatt) {
        enableWiFiConfigureNotify(gatt, false);
        mWiFiManagerService = null;
        mWiFiConfigureChar = null;
        mCharactersticConfig = null;
        waitSendData = null;
        ifbusy = false;
    }

    public void enableWiFiConfigureNotify(BluetoothGatt mGatt, boolean notifyState) {
        if (notifyState) {
            mGatt.setCharacteristicNotification(mWiFiConfigureChar, notifyState);
            mCharactersticConfig = mWiFiConfigureChar.getDescriptor(CHARACTERISTIC_CONFIGURATION);
            mCharactersticConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            internalWriteDescriptorWorkaround(mGatt, mCharactersticConfig);
        } else {
            mGatt.setCharacteristicNotification(mWiFiConfigureChar, notifyState);
            mCharactersticConfig = mWiFiConfigureChar.getDescriptor(CHARACTERISTIC_CONFIGURATION);
            mCharactersticConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            internalWriteDescriptorWorkaround(mGatt, mCharactersticConfig);
        }
    }

    private boolean internalWriteDescriptorWorkaround(BluetoothGatt mGatt, final BluetoothGattDescriptor descriptor) {
        Log.d(TAG, "internalWriteDescriptorWorkaround");
        final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
        final int originalWriteType = parentCharacteristic.getWriteType();
        parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        try {
            parentCharacteristic.setWriteType(originalWriteType);
            return mGatt.writeDescriptor(descriptor);
        } catch (Exception e) {
            Log.e(TAG, "internalWriteDescriptorWorkaround failed --> " + e);
            parentCharacteristic.setWriteType(originalWriteType);
            return false;
        }
    }

    public boolean wiFiConfigureCharWaitCmdSend(byte[] data) {
        Log.d(TAG, "wiFiConfigureCharWaitCmdSend --> " + mWiFiConfigureChar);
        if (mWiFiConfigureChar != null) {
            mWiFiConfigureChar.setValue(data);
            BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
            mBluetoothLeService.internalWriteCharacteristic(mWiFiConfigureChar);
            waitSendData = null;
            ifbusy = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean wiFiConfigureCharCmdSend(byte[] data) {
        Log.d(TAG, "wiFiConfigureCharCmdSend --> " + mWiFiConfigureChar);
        if (mWiFiConfigureChar != null) {
            mWiFiConfigureChar.setValue(data);
            if (!ifbusy) {
                BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
                mBluetoothLeService.internalWriteCharacteristic(mWiFiConfigureChar);
                ifbusy = true;
            } else {
                waitSendData = data;
            }
            return true;
        } else {
            return false;
        }
    }
}
