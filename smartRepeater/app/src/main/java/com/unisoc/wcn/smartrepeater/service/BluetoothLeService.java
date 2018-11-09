package com.unisoc.wcn.smartrepeater.service;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.unisoc.wcn.smartrepeater.data.BleDevice;

import java.util.ArrayList;
import java.util.List;

public class BluetoothLeService {
    private static final String TAG = "BluetoothLeService";
    private static Context mContext = null;
    public static BluetoothLeService mBluetoothLeService;

    private static final int MSG_LE_CONNECT = 0;
    private static final int MSG_LE_DISCONNECT = 1;
    private static final int MSG_LE_DISSERVICES = 2;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 3;
    public static final int STATE_ACL_DISCONNECTING = 4;
    public static final int STATE_SERVICEDISCOVER = 5;

    private static final int MSG_LE_GATT_SET_MTU_CMD = 10;
    private static final int MSG_PROFILE_WIFI_CMD_SEND = 11;
    private static final int MSG_LE_GATT_SET_PHY_DATA_CMD = 12;

    private LeHandler mLeHandler = null;

    public static final String ACTION_LE_CONNECTION_STATE_CHANGED = "com.unisoc.wcn.action.LE_CONNECTION_STATE_CHANGED";
    //public static final String EXTRA_STATE = "com.unisoc.wcn.extra.STATE";
    //public static final String EXTRA_DEVICE = "com.unisoc.wcn.extra.DEVICE";

    public static final String ACTION_LE_WIFI_MANAGER_DATA_NOTIFY = "com.unisoc.wcn.action.LE_WIFI_MANAGER_DATA_NOTIFY";
    public static final String LE_EXTRA_VALUE = "com.sprd.wcn.bluetoothwearable.extra.LE_VALUE";

    private List<BleDevice> mCachedDevicesList = new ArrayList<BleDevice>();
    private BleDevice mSelectedDevice = null;

    private IntentFilter mFilter;

    public void startService(Context context) {
        mContext = context;
        mBluetoothLeService = this;
        mFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mReceiver, mFilter);
        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mLeHandler = new LeHandler(ht.getLooper());
    }

    public void stopService() {
        Log.d(TAG, "stopService");
        for (int i = 0; i < mCachedDevicesList.size(); i++) {
            BleDevice cachedDevice = mCachedDevicesList.get(i);
            disconnect(cachedDevice);
        }
        mCachedDevicesList.clear();
        mSelectedDevice = null;
        mContext.unregisterReceiver(mReceiver);
        mContext = null;
    }

    public static BluetoothLeService getService() {
        return mBluetoothLeService;
    }

    public BluetoothGatt getSelectedGatt() {
        if (mSelectedDevice == null) {
            return null;
        }
        BluetoothGatt gatt = mSelectedDevice.getGatt();
        return gatt;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void requestMtu(int mtu) {
        BleDevice mDevice = getSelectedDevice();
        if (mDevice == null) {
            return;
        }
        BluetoothGatt gatt = mDevice.getGatt();
        if (gatt != null) {
            gatt.requestMtu(mtu);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void requestMtu(BluetoothGatt gatt, int mtu) {
        if (gatt != null) {
            gatt.requestMtu(mtu);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
        Log.d(TAG, "setPreferredPhy --> txPhy:" + txPhy + ";rxPhy:" + rxPhy + ";phyOptions:" + phyOptions);
        BleDevice mDevice = getSelectedDevice();
        if (mDevice == null) {
            Log.e(TAG, "setPreferredPhy --> mDevice == null");
            return;
        }
        BluetoothGatt gatt = mDevice.getGatt();
        if (gatt != null) {
            gatt.setPreferredPhy(txPhy, rxPhy, phyOptions);
        }
    }

    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.d(TAG, "onConnectionStateChange:  " + gatt.getDevice().getAddress() + " " + status + " -> " + newState);
            if (newState == STATE_CONNECTED) {
                updateStateGatt(STATE_SERVICEDISCOVER, gatt);
            } else {
                updateStateGatt(newState, gatt);
            }
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onConnectionStateChange(gatt, status, newState);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered Device --> " + gatt.getDevice().getAddress());
            BluetoothDevice device = gatt.getDevice();
            updateStateGatt(STATE_CONNECTED, gatt);
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onServicesDiscovered(gatt, status);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onCharacteristicChanged(gatt, characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onCharacteristicRead(gatt, characteristic, status);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite");
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onCharacteristicWrite(gatt, characteristic, status);
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead");
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onDescriptorRead(gatt, descriptor, status);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite");
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onDescriptorWrite(gatt, descriptor, status);
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "onReadRemoteRssi");
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onReadRemoteRssi(gatt, rssi, status);
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt,
                                 int mtu, int status) {
            Log.i(TAG, "onMtuChanged MTU --> " + mtu);
            BluetoothDevice device = gatt.getDevice();
            for (int i = 0; i < mCachedDevicesList.size(); i++) {
                BleDevice cachedDevice = mCachedDevicesList.get(i);
                if (cachedDevice.getAddress().equals(device.getAddress())) {
                    cachedDevice.onMtuChanged(gatt, mtu, status);
                }
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.i(TAG, "onPhyRead status --> " + status);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.i(TAG, "onPhyUpdate status --> " + status);
        }
    };

    public boolean internalWriteCharacteristic(final BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "internalWriteCharacteristic");
        BleDevice mDevice = getSelectedDevice();
        if (mDevice == null) {
            Log.e(TAG, "internalWriteCharacteristic failed, No Selected Device");
            return false;
        }
        final BluetoothGatt gatt = mDevice.getGatt();
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
            return false;
        }
        try {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            return gatt.writeCharacteristic(characteristic);
        } catch (Exception e) {
            Log.e(TAG, "internalWriteCharacteristic failed --> " + e);
            return false;
        }
    }

    public boolean internalWriteDescriptorWorkaround(final BluetoothGattDescriptor descriptor) {
        Log.d(TAG, "internalWriteDescriptorWorkaround");
        BleDevice mDevice = getSelectedDevice();
        if (mDevice == null) {
            Log.e(TAG, "internalWriteCharacteristic failed, No Selected Device");
            return false;
        }
        final BluetoothGatt gatt = mDevice.getGatt();
        if (gatt == null || descriptor == null)
            return false;

        final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
        final int originalWriteType = parentCharacteristic.getWriteType();
        parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        try {
            parentCharacteristic.setWriteType(originalWriteType);
            return gatt.writeDescriptor(descriptor);
        } catch (Exception e) {
            Log.e(TAG, "internalWriteDescriptorWorkaround failed --> " + e);
            parentCharacteristic.setWriteType(originalWriteType);
            return false;
        }
    }

    public boolean internalWriteCharacteristicWithoutRes(final BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "internalWriteCharacteristicWithoutRes");
        BleDevice mDevice = getSelectedDevice();
        if (mDevice == null) {
            Log.e(TAG, "internalWriteCharacteristic failed, No Selected Device");
            return false;
        }
        final BluetoothGatt gatt = mDevice.getGatt();
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0)
            return false;

        try {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            boolean state = gatt.writeCharacteristic(characteristic);
            if (!state) {
                Log.e(TAG, "internalWriteCharacteristicWithoutRes state = false ");
            }
            return state;
        } catch (Exception e) {
            Log.e(TAG, "internalWriteCharacteristicWithoutRes failed --> " + e);
            return false;
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "->: " + action);
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                for (int i = 0; i < mCachedDevicesList.size(); i++) {
                    if (mCachedDevicesList.get(i).getAddress()
                            .equals(device.getAddress())) {
                        BleDevice mDevice = mCachedDevicesList.get(i);
                        updateState(BleDevice.STATE_DISCONNECTED, mDevice);
                        break;
                    }
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (blueState == BluetoothAdapter.STATE_TURNING_OFF) {
                    Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED -> STATE_TURNING_OFF");
                } else if (blueState == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED -> STATE_ON");
                }
            }
        }
    };

    class LeHandler extends Handler {
        public LeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case MSG_LE_CONNECT: {
                    connectDevice((BleDevice) msg.obj);
                }
                break;
                case MSG_LE_DISCONNECT: {
                    disconnectDevice((BleDevice) msg.obj);
                }
                break;
                case MSG_LE_DISSERVICES: {
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    Log.d(TAG, "discovery services");
                    gatt.discoverServices();
                }
                break;
                case MSG_PROFILE_WIFI_CMD_SEND: {
                    byte[] sendData = (byte[]) msg.obj;
                    BleDevice mBleDevice = getSelectedDevice();
                    if (mBleDevice != null) {
                        boolean state = mBleDevice.wiFiConfigureCharCmdSend(sendData);
                        if (!state && (mContext != null)) {
                            Toast.makeText(mContext, "the Remote Device do not support wifiManager Profile.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
                case MSG_LE_GATT_SET_MTU_CMD: {
                    int mtu = (int) msg.obj;
                    requestMtu(mtu);
                }
                break;
                case MSG_LE_GATT_SET_PHY_DATA_CMD: {
                    Bundle phyData = (Bundle) msg.obj;
                    int txPhy = phyData.getInt("txPhy");
                    int rxPhy = phyData.getInt("rxPhy");
                    int phyOptions = phyData.getInt("phyOptions");
                    setPreferredPhy(txPhy, rxPhy, phyOptions);
                }
                break;
                default:
                    break;
            }
        }
    }

    public int connect(BleDevice device) {
        if (mCachedDevicesList.contains(device)) {
            Log.d(TAG, "device is busy");
            return 0;
        }
        updateState(BleDevice.STATE_CONNECTING, device);
        Message msg = mLeHandler.obtainMessage(MSG_LE_CONNECT);
        msg.obj = device;
        mLeHandler.sendMessage(msg);
        return 0;
    }

    public int disconnect(BleDevice device) {
        if (device.getState() != BleDevice.STATE_CONNECTED) {
            Log.d(TAG, "device is not connected");
            return 0;
        }
        updateState(BleDevice.STATE_DISCONNECTING, device);
        Message msg = mLeHandler.obtainMessage(MSG_LE_DISCONNECT);
        msg.obj = device;
        mLeHandler.sendMessage(msg);
        return 0;
    }

    private void connectDevice(BleDevice device) {
        Log.d(TAG, "Connect device: " + device.getName() + " " + device.getAddress());
        BluetoothDevice mDevice = device.getDevice();
        BluetoothGatt gatt = mDevice.connectGatt(MainService.getInstance(), false, mGattCallbacks);
        device.setGatt(gatt);
    }

    private void disconnectDevice(BleDevice device) {
        Log.d(TAG, "Disconnect device: " + device.getName() + " " + device.getAddress());
        BluetoothGatt gatt = device.getGatt();
        gatt.disconnect();
        device.stopProfiles();
        //gatt.close();
    }

    private void updateState(int newState, BleDevice device) {
        Log.d(TAG, "State: " + device.getState() + " -> " + newState);

        BleDevice mDevice = null;
        for (int i = 0; i < mCachedDevicesList.size(); i++) {
            if (mCachedDevicesList.get(i).getAddress()
                    .equals(device.getAddress())) {
                mDevice = mCachedDevicesList.get(i);
                break;
            }
        }

        if (mDevice == null) {
            device.setState(newState);
            mCachedDevicesList.add(device);
        } else {
            if (newState == BleDevice.STATE_DISCONNECTED) {
                mCachedDevicesList.remove(mDevice);
                updateSelectedDevice();
            } else {
                mDevice.setState(newState);
            }
        }

        final Intent intent = new Intent(ACTION_LE_CONNECTION_STATE_CHANGED);
        MainService.getInstance().sendBroadcast(intent);
    }

    private void updateStateGatt(int newState, BluetoothGatt gatt) {
        BluetoothDevice device = gatt.getDevice();
        BleDevice mDevice = null;
        for (int i = 0; i < mCachedDevicesList.size(); i++) {
            if (mCachedDevicesList.get(i).getAddress()
                    .equals(device.getAddress())) {
                mDevice = mCachedDevicesList.get(i);
                break;
            }
        }

        if (mDevice == null) {
            Log.d(TAG, "no found device");
        } else {
            Log.d(TAG, "State: " + mDevice.getState() + " -> " + newState);
            if (newState == BleDevice.STATE_DISCONNECTED) {
                mDevice.setGatt(null);
                mCachedDevicesList.remove(mDevice);
                updateSelectedDevice();
            } else {
                //mDevice.setGatt(gatt);
                mDevice.setState(newState);
            }
        }

        if (newState == BleDevice.STATE_SERVICEDISCOVER) {
            Message msg = mLeHandler.obtainMessage(MSG_LE_DISSERVICES);
            msg.obj = gatt;
            mLeHandler.sendMessage(msg);
        }

        final Intent intent = new Intent(ACTION_LE_CONNECTION_STATE_CHANGED);
        MainService.getInstance().sendBroadcast(intent);
    }

    public List<BleDevice> getCachedDevice() {
        return mCachedDevicesList;
    }

    public void setSelectedDevice(BleDevice theSelectedDevice) {
        mSelectedDevice = theSelectedDevice;
    }

    public BleDevice getSelectedDevice() {
        if (mCachedDevicesList.size() == 0) {
            Log.e(TAG, "getTheChoicedDevice mCachedDevicesList size == 0");
            mSelectedDevice = null;
            return null;
        } else if (mCachedDevicesList.size() == 1) {
            return mCachedDevicesList.get(0);
        } else if (mSelectedDevice != null) {
            return mSelectedDevice;
        }
        Log.e(TAG, "getTheChoicedDevice no mSelectedDevice");
        return null;
    }

    private void updateSelectedDevice() {
        if (mCachedDevicesList.size() == 0 || (mSelectedDevice == null)) {
            mSelectedDevice = null;
            return;
        }
        boolean selectedCheck = false;
        for (int i = 0; i < mCachedDevicesList.size(); i++) {
            if (mSelectedDevice.getAddress().equals(mCachedDevicesList.get(i).getAddress())) {
                selectedCheck = true;
                break;
            }
        }
        if (!selectedCheck) {
            mSelectedDevice = mCachedDevicesList.get(0);
        }
    }

    public void wifiManagerCmdSend(byte[] byteData) {
        Message msg = mLeHandler.obtainMessage(MSG_PROFILE_WIFI_CMD_SEND);
        msg.obj = byteData;
        mLeHandler.sendMessage(msg);
    }

    public void wifiManagerNotifyMessUpdate(BluetoothDevice device, byte[] value) {
        Log.d(TAG, "wifiManagerNotifyMessUpdate");
        if (mSelectedDevice == null) {
            Log.e(TAG, "no selected device");
            return;
        }
        if (device.getAddress().equals(mSelectedDevice.getAddress())) {
            Log.d(TAG, "wifiManagerNotifyMessUpdate , value --> " + value.toString());
            final Intent intent = new Intent(ACTION_LE_WIFI_MANAGER_DATA_NOTIFY);
            intent.putExtra(LE_EXTRA_VALUE, value);
            MainService.getInstance().sendBroadcast(intent);
        } else {
            Log.e(TAG, "wrong selected device");
        }
    }

    public void setMtu(int mtu) {
        Message msg = mLeHandler.obtainMessage(MSG_LE_GATT_SET_MTU_CMD);
        msg.obj = mtu;
        mLeHandler.sendMessage(msg);
    }
}
