package com.unisoc.wcn.smartrepeater.profiles;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by clay.zhu on 2018/7/31.
 */

public interface BleProfile {

    public static final int WIFIMANAGER = 0;

    public void enable(boolean enable);

    public int getProfile();

    public boolean start(BluetoothGatt gatt);

    public void stop(BluetoothGatt gatt);

    public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                        int newState);

    public void onServicesDiscovered(BluetoothGatt gatt, int status);

    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic);

    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status);

    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status);

    public void onDescriptorRead(BluetoothGatt gatt,
                                 BluetoothGattDescriptor descriptor, int status);

    public void onDescriptorWrite(BluetoothGatt gatt,
                                  BluetoothGattDescriptor descriptor, int status);

    public void onReadRemoteRssi(BluetoothGatt gatt,
                                 int rssi, int status);

    public void onMtuChanged(BluetoothGatt gatt,
                             int mtu, int status);
}
