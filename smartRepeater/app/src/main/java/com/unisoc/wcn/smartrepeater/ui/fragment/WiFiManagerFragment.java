package com.unisoc.wcn.smartrepeater.ui.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.unisoc.wcn.smartrepeater.R;
import com.unisoc.wcn.smartrepeater.data.BleDevice;
import com.unisoc.wcn.smartrepeater.data.ScanWiFiData;
import com.unisoc.wcn.smartrepeater.data.Utils;
import com.unisoc.wcn.smartrepeater.service.BluetoothLeService;
import com.unisoc.wcn.smartrepeater.service.MainService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WiFiManagerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private String TAG = "WiFiManagerFragment";

    private static final byte OPEN_WIFI_RES = (byte) 0x81;
    private static final byte CLOSE_WIFI_RES = (byte) 0x82;
    private static final byte SET_CONF_WIFI_RES = (byte) 0x83;
    private static final byte DISCONNECT_WIFI_RES = (byte) 0x84;
    private static final byte GET_STATE_WIFI_RES = (byte) 0x85;
    private static final byte GET_CONF_WIFI_RES = (byte) 0x86;
    private static final byte GET_START_AP_RES = (byte) 0x87;

    private static final byte WIFI_SCAN_STATE_RES = (byte) 0x89;
    private static final byte WIFI_SCAN_RESULT_RES = (byte) 0x8A;

    private static final byte CMD_STATE_SUCCESSFUL_RES = (byte) 0x00;
    private static final byte CMD_STATE_FAILED_RES = (byte) 0x01;

    private TextView mWiFiState = null;
    private Button mWiFiManager = null;
    private EditText mWiFiSsid = null;
    private EditText mWiFiPassword = null;
    private TextView mConnectedWiFiMessage = null;
    private Button mSetConf = null;

    private TextView mWiFiStaAddress = null;
    private TextView mWiFiApAddress = null;

    private Button mSoftApStart = null;
    private Button mCloseRemoteBt = null;
    private Button mStartWifiScan = null;

    private IntentFilter mFilter;

    private Spinner mBTSelected;
    private Spinner mWifiSsidList;
    private List<String> selectedDeviceLsit = new ArrayList<String>();
    private ArrayAdapter<String> selectedDeviceLsitAdapter;

    private MainService mMainService = null;
    List<BleDevice> allDevice = new ArrayList<BleDevice>();

    private List<ScanResult> mWiFiResultList = null;
    private List<ScanWiFiData> mScanWiFiDataList = new ArrayList<ScanWiFiData>();
    private int mChoicedWiFi = 0;

    Dialog mDialog = null;
    Dialog mWifiScanDialog = null;

    private boolean remoteDeviceWiFiState = false;
    private boolean ifConnectedWiFi = false;

    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    private static final String WIFI_AUTH_OPEN = "";
    private static final String WIFI_AUTH_ROAM = "[ESS]";
    private static final String WIFI_AUTH_OPEN_ROAM = "[WPS][ESS]";
    private final ArrayList<String> wifiList = new ArrayList<>();

    private String logMess = "";

    // TODO: Rename and change types of parameters
    private String mParam1;

    private static Handler mRssiHandler = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wi_fi_manager, container, false);
        init(view);
        if (mMainService == null) {
            mMainService = MainService.getInstance();
        }
        wifiList.clear();
        mScanWiFiDataList.clear();
        initBroadcast();
        initDeviceSelected();
//        initWifiAdmin();
        return view;
    }

    private void init(View view) {
        mWiFiState = (TextView) view.findViewById(R.id.wifi_state);
        mConnectedWiFiMessage = (TextView) view.findViewById(R.id.connected_wifi_message);
        mWiFiManager = (Button) view.findViewById(R.id.wifi_manager);
        mWiFiManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open or close wifi
                sendWiFiStateControlCmd(remoteDeviceWiFiState);
            }
        });
        mWiFiSsid = (EditText) view.findViewById(R.id.wifi_ssid);
        mWiFiPassword = (EditText) view.findViewById(R.id.wifi_password);
        mSetConf = (Button) view.findViewById(R.id.set_conf);
        mSetConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ssidAndPassSubmit();
                try {
                    InputMethodManager imm = (InputMethodManager)
                            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                } catch (Exception e) {
                    Log.e(TAG, "hind keyBoard error --> " + e);
                }
            }
        });

        mSoftApStart = (Button) view.findViewById(R.id.soft_ap_start);
        mSoftApStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ifConnectedWiFi) {
                    sendSoftApStartCmd();
                } else {
                    Toast.makeText(getContext(), "Remote Device No Connected Wifi.", Toast.LENGTH_LONG).show();
                }
            }
        });
        mCloseRemoteBt = (Button) view.findViewById(R.id.close_remote_bt);
        mCloseRemoteBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeRemoteDeviceBtCmd();
            }
        });

        mStartWifiScan = (Button) view.findViewById(R.id.start_wifi_scan);
        mStartWifiScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (remoteDeviceWiFiState) {
                    startWiFiScanCms();
                } else {
                    Toast.makeText(getContext(), "Please Open WiFi of Remote Device First..", Toast.LENGTH_LONG).show();
                }

            }
        });

        mWiFiStaAddress = (TextView) view.findViewById(R.id.wifi_sta_address);
        mWiFiApAddress = (TextView) view.findViewById(R.id.wifi_ap_address);

        mBTSelected = (Spinner) view.findViewById(R.id.bt_manager_selected);
        mBTSelected.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                Log.d(TAG, "mBTSelected , onItemSelected --> " + arg2);
                if (allDevice.size() != 0) {
                    Log.d(TAG, "mBTSelected , onItemSelected --> " + allDevice.get(arg2).getAddress());
                    updateSelectedDevice(allDevice.get(arg2));
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mWifiSsidList = (Spinner) view.findViewById(R.id.wifi_ssid_list);
        mWifiSsidList.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // TODO Auto-generated method stub
                Log.d(TAG, "mWifiSsidList , onItemSelected --> " + arg2);
                mChoicedWiFi = arg2;
                // for mWiFiResultList
//                if (mWiFiResultList.size() > 0) {
//                    String fiwiInfo = mWiFiResultList.get(mChoicedWiFi).SSID;
//                    mWiFiSsid.setText(fiwiInfo);
//                }
                //for mScanWiFiDataList
                if (mScanWiFiDataList.size() > 0) {
                    String fiwiInfo = mScanWiFiDataList.get(mChoicedWiFi).SSID;
                    mWiFiSsid.setText(fiwiInfo);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void initBroadcast() {
        mFilter = new IntentFilter();
        mFilter.addAction(BluetoothLeService.ACTION_LE_CONNECTION_STATE_CHANGED);
        mFilter.addAction(BluetoothLeService.ACTION_LE_WIFI_MANAGER_DATA_NOTIFY);
        this.getContext().registerReceiver(mReceiver, mFilter);
    }

    private void initDeviceSelected() {
        selectedDeviceLsit.clear();
        allDevice = mMainService.getCachedDevice();
        BleDevice selectedDevice = mMainService.getSelectedDevice();
        int selectedItemNum = 0;
        if (allDevice.size() == 0) {
            selectedDeviceLsit.add("No Connected Device!");
            addressShowInit(null);
        }
        for (int i = 0; i < allDevice.size(); i++) {
            String listMess = allDevice.get(i).getName() + "(" + allDevice.get(i).getAddress() + ")";
            selectedDeviceLsit.add(listMess);
            if (selectedDevice != null) {
                if (selectedDevice.getAddress().equals(allDevice.get(i).getAddress())) {
                    selectedItemNum = i;
                }
            }
        }
        selectedDeviceLsitAdapter = new ArrayAdapter<String>(this.getContext(),
                android.R.layout.simple_spinner_item, selectedDeviceLsit);
        selectedDeviceLsitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBTSelected.setAdapter(selectedDeviceLsitAdapter);
        mBTSelected.setSelection(selectedItemNum);
        if (allDevice.size() != 0) {
            updateSelectedDevice(allDevice.get(selectedItemNum));
            initRemoteDeviceWiFiInfo();
        } else {
            Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
        }
    }

    public void initWifiAdmin() {
        Log.d(TAG, "initWifiAdmin");
        mWifiManager = (WifiManager) this.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();
        String connectedWiFiSsid = mWifiInfo.getSSID();
        Log.d(TAG, "get connected wifi ssid --> " + connectedWiFiSsid);
//        if (connectedWiFiSsid.equals("<unknown ssid>")) {
//            Toast.makeText(getContext(), "Your Device WiFI is not Connected.", Toast.LENGTH_LONG).show();
//        }
        mWiFiResultList = mWifiManager.getScanResults();
        wifiList.clear();
        Log.d(TAG, "mWiFiResultList len --> " + mWiFiResultList.size());
        for (int i = 0; i < mWiFiResultList.size(); i++) {
            ScanResult resulrWifi = mWiFiResultList.get(i);
            String wifiSsid = resulrWifi.SSID;
            if (wifiSsid.equals("")) {
                mWiFiResultList.remove(resulrWifi);
                continue;
            }
        }
        for (int i = 0; i < mWiFiResultList.size(); i++) {
            ScanResult resulrWifi = mWiFiResultList.get(i);
            String wifiSsid = resulrWifi.SSID;
            String wifiBSsid = resulrWifi.BSSID;
            String capabilities = resulrWifi.capabilities.trim();
//            if ((capabilities.equals(WIFI_AUTH_OPEN) || capabilities.equals(WIFI_AUTH_ROAM) || capabilities.equals(WIFI_AUTH_OPEN_ROAM))) {
//                if (!wifiList.contains(wifiSsid + "(open)")) {
//                    wifiList.add(wifiSsid + "(open)\n" + wifiBSsid);
//                }
//            } else {
//                if (!wifiList.contains(wifiSsid)) {
//                    wifiList.add(wifiSsid + "\n" + wifiBSsid);
//                }
//            }
            String wifiMess = wifiSsid + "\n" + wifiBSsid;
            if (!wifiList.contains(wifiMess)) {
                wifiList.add(wifiMess);
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_single_choice, wifiList);
        mWifiSsidList.setAdapter(adapter);
    }

    private void initRemoteDeviceWiFiInfo() {
        if (mDialog != null) {
            Utils.closeDialog(mDialog);
            mDialog = null;
        }
        mDialog = Utils.createLoadingDialog(this.getContext(), "loading state...");
        dialogShowTimeOut(4000);
        sendGetStateCmd();
    }

    private void updateSelectedDevice(BleDevice device) {
        if (mMainService == null) {
            mMainService = MainService.getInstance();
        }
        if (mMainService == null) {
            return;
        }
        mMainService.setSelectedDevice(device);
    }

    private void sendWiFiStateControlCmd(boolean state) {
        Log.d(TAG, "sendOpenWiFiCmd");
        byte[] sendDataBytes = wifiStateControlBytesGet(state);
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void ssidAndPassSubmit() {
        Log.d(TAG, "ssidAndPassSubmit");
        String ssid = "";
        String bssid = "";
        String password = "";
        // for mWiFiResultList
//        if (mWiFiResultList.size() == 0) {
//            Log.e(TAG, "ssidAndPassSubmit error --> mWiFiResultList.size() == 0");
//            ssid = mWiFiSsid.getText().toString().trim();
//            bssid = "";
//            password = mWiFiPassword.getText().toString().trim();
//        } else {
//            String showSsid = mWiFiSsid.getText().toString().trim();
//            ssid = mWiFiResultList.get(mChoicedWiFi).SSID;
//            if (showSsid.equals(ssid)) {
//                bssid = mWiFiResultList.get(mChoicedWiFi).BSSID;
//            } else {
//                bssid = "";
//            }
//            password = mWiFiPassword.getText().toString().trim();
//        }
        //for mScanWiFiDataList
        if (mScanWiFiDataList.size() == 0) {
            Log.e(TAG, "ssidAndPassSubmit error --> mScanWiFiDataList.size() == 0");
            ssid = mWiFiSsid.getText().toString().trim();
            bssid = "";
            password = mWiFiPassword.getText().toString().trim();
        } else {
            String showSsid = mWiFiSsid.getText().toString().trim();
            ssid = mScanWiFiDataList.get(mChoicedWiFi).SSID;
            if (showSsid.equals(ssid)) {
                bssid = mScanWiFiDataList.get(mChoicedWiFi).BSSID;
            } else {
                bssid = "";
            }
            password = mWiFiPassword.getText().toString().trim();
        }
        Log.d(TAG, "ssid --> " + ssid + "; bssid --> " + bssid + "; password --> " + password);
        String bssidSend = bssid.replace(":", "");
        byte[] ssidBytes = Utils.strToByteArray(ssid);
        byte[] bssidBytes = Utils.hexStrToByteArray(bssidSend);
        byte[] passBytes = Utils.strToByteArray(password);
        byte[] sendDataBytes = ssidAndPassBytesGet(ssidBytes, bssidBytes, passBytes);
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                if (mDialog != null) {
                    Utils.closeDialog(mDialog);
                    mDialog = null;
                }
                mDialog = Utils.createLoadingDialog(this.getContext(), "connecting...");
                dialogShowTimeOut(8000);
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendDisconnectWiFiCmd() {
        Log.d(TAG, "sendOpenWiFiCmd");
        byte[] sendDataBytes = disconnectWiFiBytesGet();
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendGetStateCmd() {
        Log.d(TAG, "sendGetStateCmd");
        byte[] sendDataBytes = getStateCmdBytesGet();
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendGetConfInfoCmd() {
        Log.d(TAG, "sendGetConfInfoCmd");
        byte[] sendDataBytes = getConfInfoCmdBytesGet();
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendSoftApStartCmd() {
        Log.d(TAG, "sendSoftApStartCmd");
        byte[] sendDataBytes = getSoftApStartBytesGet();
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void closeRemoteDeviceBtCmd() {
        Log.d(TAG, "closeRemoteDeviceBtCmd");
        byte[] sendDataBytes = getCloseRemoteBtBytesGet();
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startWiFiScanCms() {
        Log.d(TAG, "startWiFiScanCms");
        byte[] sendDataBytes = getStartWiFiScanBtBytesGet();
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        if (mBluetoothLeService != null) {
            if (mBluetoothLeService.getSelectedDevice() != null) {
                if (mWifiScanDialog != null) {
                    Utils.closeDialog(mWifiScanDialog);
                    mWifiScanDialog = null;
                }
                wifiList.clear();
                mScanWiFiDataList.clear();
                mWifiScanDialog = Utils.createLoadingDialog(this.getContext(), "loading wifi message...");
                wifiScanDialogShowTimeOut(8000);
                mBluetoothLeService.wifiManagerCmdSend(sendDataBytes);
            } else {
                Toast.makeText(getContext(), "No Connected Device.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private byte[] wifiStateControlBytesGet(boolean state) {
        byte[] sendDataBytes = new byte[4];
        if (state) {
            sendDataBytes[0] = 0x02;
        } else {
            sendDataBytes[0] = 0x01;
        }
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private byte[] disconnectWiFiBytesGet() {
        byte[] sendDataBytes = new byte[4];
        sendDataBytes[0] = 0x04;
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private byte[] getStateCmdBytesGet() {
        byte[] sendDataBytes = new byte[4];
        sendDataBytes[0] = 0x05;
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private byte[] getConfInfoCmdBytesGet() {
        byte[] sendDataBytes = new byte[4];
        sendDataBytes[0] = 0x06;
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private byte[] ssidAndPassBytesGet(byte[] ssidBytes, byte[] bSsidBytes, byte[] passBytes) {
        int ssidBytesLen = ssidBytes.length;
        int bSsidBytesLen = bSsidBytes.length;
        int passBytesLen = passBytes.length;
        int sendDataLen = 9 + ssidBytesLen + bSsidBytesLen + passBytesLen;
        byte[] sendDataBytes = new byte[sendDataLen];
        sendDataBytes[0] = 0x03;
        System.arraycopy(Utils.int2bytes_two(sendDataLen - 3), 0, sendDataBytes, 1, 2);
        //ssid
        System.arraycopy(Utils.int2bytes_two(ssidBytesLen), 0, sendDataBytes, 3, 2);
        System.arraycopy(ssidBytes, 0, sendDataBytes, 5, ssidBytesLen);
        //bssid
        System.arraycopy(Utils.int2bytes_two(bSsidBytesLen), 0, sendDataBytes, ssidBytesLen + 5, 2);
        System.arraycopy(bSsidBytes, 0, sendDataBytes, ssidBytesLen + 7, bSsidBytesLen);
        //passwd
        System.arraycopy(Utils.int2bytes_two(passBytesLen), 0, sendDataBytes, ssidBytesLen + bSsidBytesLen + 7, 2);
        System.arraycopy(passBytes, 0, sendDataBytes, ssidBytesLen + bSsidBytesLen + 9, passBytesLen);
        return sendDataBytes;
    }

    private byte[] getSoftApStartBytesGet() {
        byte[] sendDataBytes = new byte[4];
        sendDataBytes[0] = 0x07;
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private byte[] getCloseRemoteBtBytesGet() {
        byte[] sendDataBytes = new byte[4];
        sendDataBytes[0] = 0x08;
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private byte[] getStartWiFiScanBtBytesGet() {
        byte[] sendDataBytes = new byte[4];
        sendDataBytes[0] = 0x09;
        sendDataBytes[1] = 0x01;
        sendDataBytes[2] = 0x00;
        sendDataBytes[3] = 0x00;
        return sendDataBytes;
    }

    private void setMtu(int value) {
        BluetoothLeService mBluetoothLeService = BluetoothLeService.getService();
        mBluetoothLeService.setMtu(value);
    }

    private void wifiManagerNotify(byte[] value) {
        byte opcocde = value[0];
        switch (opcocde) {
            case OPEN_WIFI_RES: {
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
                    mWiFiState.setText("WIFI STATE: Ready");
                    remoteDeviceWiFiState = true;
                    mWiFiManager.setText("CLOSE WIFI");
                } else if (result == CMD_STATE_FAILED_RES) {
                    addressShowInit(null);
                    mWiFiState.setText("WIFI STATE: Closed");
                    remoteDeviceWiFiState = false;
                    mWiFiManager.setText("OPEN WIFI");
                }
            }
            break;
            case CLOSE_WIFI_RES: {
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    addressShowInit(null);
                    mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
                    mWiFiState.setText("WIFI STATE: Closed");
                    remoteDeviceWiFiState = false;
                    mWiFiManager.setText("OPEN WIFI");
                } else if (result == CMD_STATE_FAILED_RES) {
                    mWiFiState.setText("WIFI STATE: Ready");
                    remoteDeviceWiFiState = true;
                    mWiFiManager.setText("CLOSE WIFI");
                }
            }
            break;
            case SET_CONF_WIFI_RES: {
                if (mDialog != null) {
                    Utils.closeDialog(mDialog);
                    mDialog = null;
                }
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    mWiFiState.setText("WIFI STATE: Connected");
                    remoteDeviceWiFiState = true;
                    mWiFiManager.setText("CLOSE WIFI");
                    ifConnectedWiFi = true;
                    sendGetStateCmd();
                    Toast.makeText(getContext(), "Connection Success...", Toast.LENGTH_LONG).show();
                } else if (result == CMD_STATE_FAILED_RES) {
                    ifConnectedWiFi = false;
                    sendGetStateCmd();
                    Toast.makeText(getContext(), "Connection Failed...", Toast.LENGTH_LONG).show();
                }
            }
            break;
            case DISCONNECT_WIFI_RES: {
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
                    Toast.makeText(getContext(), "Disconnect Successful...", Toast.LENGTH_LONG).show();
                    ifConnectedWiFi = false;
                } else if (result == CMD_STATE_FAILED_RES) {
                    Toast.makeText(getContext(), "Disconnect Failed...", Toast.LENGTH_LONG).show();
                }
                sendGetStateCmd();
            }
            break;
            case GET_STATE_WIFI_RES: {
                if (mDialog != null) {
                    Utils.closeDialog(mDialog);
                    mDialog = null;
                }
                byte result = value[1];
                mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
                if (result == CMD_STATE_SUCCESSFUL_RES) {
//                    Toast.makeText(getContext(), "get State Successful...", Toast.LENGTH_LONG).show();
                    int state = value[2];
                    if (state == 0x00) {
                        mWiFiState.setText("WIFI STATE: Closed");
                        remoteDeviceWiFiState = false;
                        mWiFiManager.setText("OPEN WIFI");
                    } else {
                        mWiFiState.setText("WIFI STATE: Ready");
                        remoteDeviceWiFiState = true;
                        mWiFiManager.setText("CLOSE WIFI");
                        if (state == 0x04) {
                            mWiFiState.setText("WIFI STATE: Connected");
                            sendGetConfInfoCmd();
                            ifConnectedWiFi = true;
                        } else {
                            ifConnectedWiFi = false;
                            mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
                        }
                    }
                    addressShowInit(value);
                } else if (result == CMD_STATE_FAILED_RES) {
                    addressShowInit(null);
                    Toast.makeText(getContext(), "get State Failed...", Toast.LENGTH_LONG).show();
                }
            }
            break;
            case GET_CONF_WIFI_RES: {
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    ifConnectedWiFi = true;
                    mConnectedWiFiMessage.setText(parseConnectedWiFiMess(value));
                } else if (result == CMD_STATE_FAILED_RES) {
                    ifConnectedWiFi = false;
                    mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
                    Toast.makeText(getContext(), "get Conf Failed...", Toast.LENGTH_LONG).show();
                }
            }
            break;
            case GET_START_AP_RES: {
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    Toast.makeText(getContext(), "Start AP Successful...", Toast.LENGTH_LONG).show();
                } else if (result == CMD_STATE_FAILED_RES) {
                    Toast.makeText(getContext(), "Start AP Failed...", Toast.LENGTH_LONG).show();
                }
            }
            break;
            case WIFI_SCAN_STATE_RES: {
                if (mWifiScanDialog != null) {
                    Utils.closeDialog(mWifiScanDialog);
                    mWifiScanDialog = null;
                }
            }
            break;
            case WIFI_SCAN_RESULT_RES: {
                byte result = value[1];
                if (result == CMD_STATE_SUCCESSFUL_RES) {
                    parseWifiScanResult(value);
                } else if (result == CMD_STATE_FAILED_RES) {
                    Toast.makeText(getContext(), "wifi scan resulr failed...", Toast.LENGTH_LONG).show();
                }
            }
            break;
            default: {

            }
            break;
        }
    }

    private String parseConnectedWiFiMess(byte[] value) {
        Log.d(TAG, "parseConnectedWiFiMess");
        byte[] allDataLenByte = new byte[2];
        System.arraycopy(value, 2, allDataLenByte, 0, 2);
        int allDataLen = Utils.bytes2int_two(allDataLenByte);
        Log.d(TAG, "allDataLen --> " + allDataLen);

        //get ssid
        byte[] ssidDataLenByte = new byte[2];
        System.arraycopy(value, 4, ssidDataLenByte, 0, 2);
        int ssidDataLen = Utils.bytes2int_two(ssidDataLenByte);
        Log.d(TAG, "ssidDataLen --> " + ssidDataLen);
        byte[] ssidDataByte = new byte[ssidDataLen];
        System.arraycopy(value, 6, ssidDataByte, 0, ssidDataLen);
        String ssidData = Utils.byteArrayToStr(ssidDataByte);
        Log.d(TAG, "ssidData --> " + ssidData);

//        //get bssid
//        byte[] bssidDataLenByte = new byte[2];
//        System.arraycopy(value, 6 + ssidDataLen, bssidDataLenByte, 0, 2);
//        int bssidDataLen = Utils.bytes2int_two(bssidDataLenByte);
//        Log.d(TAG, "bssidDataLen --> " + bssidDataLen);
//        byte[] bssidDataByte = new byte[bssidDataLen];
//        System.arraycopy(value, 8 + ssidDataLen, bssidDataByte, 0, bssidDataLen);
//        String bssidData = Utils.byteArrayToStr(bssidDataByte);
//        Log.d(TAG, "bssidData --> " + bssidData);
//
//        //get passwd
//        byte[] passwdDataLenByte = new byte[2];
//        System.arraycopy(value, 8 + ssidDataLen + bssidDataLen, passwdDataLenByte, 0, 2);
//        int passwdbssidDataLen = Utils.bytes2int_two(passwdDataLenByte);
//        Log.d(TAG, "passwdbssidDataLen --> " + passwdbssidDataLen);
//        byte[] passwdDataByte = new byte[passwdbssidDataLen];
//        System.arraycopy(value, 10 + ssidDataLen + bssidDataLen, passwdDataByte, 0, passwdbssidDataLen);
//        String passwdData = Utils.byteArrayToStr(passwdDataByte);
//        Log.d(TAG, "passwdData --> " + passwdData);
        String messShow = "CONNECTED WIFI: " + ssidData;
        return messShow;
    }

    private void parseWifiScanResult(byte[] value) {
        Log.d(TAG, "parseWifiScanResult");
        byte[] allDataLenByte = new byte[2];
        System.arraycopy(value, 2, allDataLenByte, 0, 2);
        int allDataLen = Utils.bytes2int_two(allDataLenByte);
        Log.d(TAG, "allDataLen --> " + allDataLen);

        //get ssid
        byte[] ssidDataLenByte = new byte[2];
        System.arraycopy(value, 4, ssidDataLenByte, 0, 2);
        int ssidDataLen = Utils.bytes2int_two(ssidDataLenByte);
        Log.d(TAG, "ssidDataLen --> " + ssidDataLen);
        byte[] ssidDataByte = new byte[ssidDataLen];
        System.arraycopy(value, 6, ssidDataByte, 0, ssidDataLen);
        String ssidData = Utils.byteArrayToStr(ssidDataByte);
        Log.d(TAG, "ssidData --> " + ssidData);

        //get bssid
        byte[] bssidDataLenByte = new byte[2];
        System.arraycopy(value, 6 + ssidDataLen, bssidDataLenByte, 0, 2);
        int bssidDataLen = Utils.bytes2int_two(bssidDataLenByte);
        Log.d(TAG, "bssidDataLen --> " + bssidDataLen);
        byte[] bssidDataByte = new byte[bssidDataLen];
        System.arraycopy(value, 8 + ssidDataLen, bssidDataByte, 0, bssidDataLen);
        String bssidData = Utils.ByteArrayToMacStr(bssidDataByte);
//        String bssidData = Utils.byteArrayToStr(bssidDataByte);
        Log.d(TAG, "bssidData --> " + bssidData);

        String wifiMess = ssidData + "\n" + bssidData;
        if (!wifiList.contains(wifiMess)) {
            ScanWiFiData mScanWifiMess = new ScanWiFiData();
            try {
                mScanWifiMess.SSID = ssidData;
                mScanWifiMess.BSSID = bssidData;
                mScanWiFiDataList.add(mScanWifiMess);
                wifiList.add(wifiMess);
            } catch (Exception e) {
                Log.e(TAG, "mScanWifiMess error --> " + e);
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_single_choice, wifiList);
        mWifiSsidList.setAdapter(adapter);
    }

    private void dialogShowTimeOut(int outTime) {
        final int time = outTime;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(time);
                    if (mDialog != null) {
                        Utils.closeDialog(mDialog);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void wifiScanDialogShowTimeOut(int outTime) {
        final int time = outTime;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(time);
                    if (mWifiScanDialog != null) {
                        Utils.closeDialog(mWifiScanDialog);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addressShowInit(byte[] value) {
        if (value == null) {
            mWiFiStaAddress.setText("STATE ADDR: xx:xx:xx:xx:xx:xx");
            mWiFiApAddress.setText("AP ADDR: xx:xx:xx:xx:xx:xx");
            mWiFiState.setText("WIFI STATE: ---");
            mConnectedWiFiMessage.setText("CONNECTED WIFI: ---");
        } else {
            byte[] staAddrByte = new byte[6];
            byte[] apAddrByte = new byte[6];
            System.arraycopy(value, 3, staAddrByte, 0, 6);
            System.arraycopy(value, 9, apAddrByte, 0, 6);
            mWiFiStaAddress.setText("STATE ADDR: " + Utils.ByteArrayToMacStr(staAddrByte));
            mWiFiApAddress.setText("AP ADDR: " + Utils.ByteArrayToMacStr(apAddrByte));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        mFilter = null;
        this.getContext().unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "->: " + action);
            if (BluetoothLeService.ACTION_LE_CONNECTION_STATE_CHANGED.equals(action)) {
                initDeviceSelected();
            } else if (BluetoothLeService.ACTION_LE_WIFI_MANAGER_DATA_NOTIFY.equals(action)) {
                byte[] value = (byte[]) intent.getByteArrayExtra(BluetoothLeService.LE_EXTRA_VALUE);
                Log.d(TAG, "value --> " + Arrays.toString(value));
                logMess = logMess + "\n" + Utils.printHexString(value);
                wifiManagerNotify(value);
            }
        }
    };

}
