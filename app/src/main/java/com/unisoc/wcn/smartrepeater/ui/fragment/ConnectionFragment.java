package com.unisoc.wcn.smartrepeater.ui.fragment;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.unisoc.wcn.smartrepeater.R;
import com.unisoc.wcn.smartrepeater.data.BleDevice;
import com.unisoc.wcn.smartrepeater.data.Utils;
import com.unisoc.wcn.smartrepeater.service.BluetoothLeService;
import com.unisoc.wcn.smartrepeater.service.MainService;
import com.unisoc.wcn.smartrepeater.ui.base.AvailableDevicesListAdapter;
import com.unisoc.wcn.smartrepeater.ui.base.CachedDevicesListAdapter;

import java.util.ArrayList;
import java.util.List;

public class ConnectionFragment extends Fragment {
    private static final String TAG = "BTConnectFragment";
    private static final String ARG_PARAM = "param";
    private RotateAnimation animation;
    private BluetoothAdapter mBluetoothAdapter = null;
    private List<BleDevice> mAvailableDevices = new ArrayList<BleDevice>();
    private boolean scanState = false;
    private RecyclerView mAvailableDevicesView;
    private ListView mCachedDevicesView;
    private TextView mSelectedDevice = null;
    private AvailableDevicesListAdapter mAvailableDevicesListAdapter = null;
    private CachedDevicesListAdapter mCachedDevicesListAdapter = null;
    private MainService mMainService = null;
    private FloatingActionButton fab = null;
    private IntentFilter mFilter;

    private View inflate;
    private TextView wifiManager;
    private Dialog dialog;

    public ConnectionFragment() {
        // Required empty
    }

    public static ConnectionFragment newInstance(String param1, String param2) {
        ConnectionFragment fragment = new ConnectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String mParam1 = getArguments().getString(ARG_PARAM);
        }

        animation =
                new RotateAnimation(0, 359, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(100);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.RESTART);
        init();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        this.getContext().unregisterReceiver(mReceiver);
        if (scanState)
            scanEnable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.connection_fragment, container, false);

        mAvailableDevicesView = (RecyclerView) view.findViewById(R.id.available_devices_list);
        mAvailableDevicesListAdapter = new AvailableDevicesListAdapter(this.getContext(), mAvailableDevices);
        mAvailableDevicesListAdapter.setmOnAvailableDeviceListener(new Utils.OnAvailableDeviceListener() {
            @Override
            public void onClick(BleDevice device) {
                if (scanState)
                    scanEnable(false);
                Log.d(TAG, "Click device: " + device.getName() + " " + device.getAddress());
                if (mMainService == null) {
                    mMainService = MainService.getInstance();
                }

                mMainService.connect(device);

            }
        });
        mAvailableDevicesView.setAdapter(mAvailableDevicesListAdapter);
        mCachedDevicesView = (ListView) view.findViewById(R.id.cached_devices_list);
        mCachedDevicesView.setClickable(true);
        mCachedDevicesView.setOnItemClickListener(mCachedDevicesClickListener);
        mCachedDevicesView.setOnItemLongClickListener(mCachedDevicesLongClickListener);

        mSelectedDevice = (TextView) view.findViewById(R.id.selected_device);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!scanState) {
                    scanEnable(true);
                } else {
                    scanEnable(false);
                }
            }
        });
        notifyCachedDevicesListSetChanged();
        updateSelectedDevice();
        return view;
    }

    private void init() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mFilter = new IntentFilter();
        mFilter.addAction(BluetoothLeService.ACTION_LE_CONNECTION_STATE_CHANGED);
        this.getContext().registerReceiver(mReceiver, mFilter);
    }

    private void updateAvailableDevices(BluetoothDevice device, int rssi) {
        int position = -1;
        for (position = 0; position < mAvailableDevices.size(); position++) {
            if (mAvailableDevices.get(position)
                    .equalsDevice(device)) {
                break;
            }
        }

        if (position == mAvailableDevices.size()) {
            mAvailableDevices.add(new BleDevice(device, rssi));
            Log.d(TAG, "Found device: " + device.getName() + "; Address:" + device.getAddress());
            notifyAvailableDevicesListSetChanged(position);
            return;
        }

        if (mAvailableDevices.get(position).isNameEmpty() && device.getName() != null
                && device.getName().length() != 0) {
            mAvailableDevices.get(position).update(device, rssi);
            notifyAvailableDevicesListSetChanged(position, true);
        } else {
            mAvailableDevices.get(position).updateRssi(rssi);
            notifyAvailableDevicesListSetChanged(position, false);
        }

    }

    private void notifyAvailableDevicesListSetChanged() {
        mAvailableDevicesListAdapter.notifyDataSetChanged();
    }

    private void notifyAvailableDevicesListSetChanged(int position) {
        mAvailableDevicesListAdapter.notifyItemChanged(position);
    }

    private void notifyAvailableDevicesListSetChanged(int position, boolean upName) {
        Integer update = BleDevice.UP_RSSI;
        if (upName) {
            update = BleDevice.UP_NAME_RSSI;
        }
        mAvailableDevicesListAdapter.notifyItemChanged(position, update);
    }

    private void notifyCachedDevicesListSetChanged() {
        if (mMainService == null) {
            mMainService = MainService.getInstance();
        }
        if (mMainService == null) {
            return;
        }
        if (mCachedDevicesListAdapter == null) {
            mCachedDevicesListAdapter = new CachedDevicesListAdapter(this.getContext(), mMainService.getCachedDevice());
            mCachedDevicesView.setAdapter(mCachedDevicesListAdapter);
        }
        mCachedDevicesListAdapter.notifyDataSetChanged();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Log.d(TAG, "Ble Device Found --> Name:" + device.getName() + "; Address:" + device.getAddress());
            updateAvailableDevices(device, rssi);
        }
    };

    private void scanEnable(boolean enable) {
        if (mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Bluetooth is not open, please open the Bluetooth first...", Toast.LENGTH_LONG).show();
            return;
        }
        if (enable) {
            Log.d(TAG, "Start Scan");
            mAvailableDevices.clear();
            notifyAvailableDevicesListSetChanged();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            fab.setAnimation(animation);
            fab.startAnimation(animation);
        } else {
            Log.d(TAG, "Stop Scan");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            fab.clearAnimation();
        }
        scanState = enable;
    }

    private AdapterView.OnItemClickListener mCachedDevicesClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            if (mMainService == null) {
                mMainService = MainService.getInstance();
            }

            BleDevice device = mMainService.getCachedDevice().get(position);
            Log.d(TAG, "Click Device: " + device.getName() + " " + device.getAddress());
            if (scanState) {
                scanEnable(false);
            }
            if (mMainService == null) {
                mMainService = MainService.getInstance();
            }

            mMainService.disconnect(device);
        }
    };

    private AdapterView.OnItemLongClickListener mCachedDevicesLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                       long id) {
            if (mMainService == null) {
                mMainService = MainService.getInstance();
            }
            BleDevice device = mMainService.getCachedDevice().get(position);
            mMainService.setSelectedDevice(device);
            updateSelectedDevice();
            dialogShowSelected();
            return true;
        }
    };

    private void updateSelectedDevice() {
        if (mMainService == null) {
            mMainService = MainService.getInstance();
        }
        if (mMainService == null) {
            mSelectedDevice.setText("NULL");
            return;
        }
        BleDevice device = mMainService.getSelectedDevice();
        if (device == null) {
            mSelectedDevice.setText("NULL");
            return;
        }
        String name = device.getName();
        String address = device.getAddress();
        mSelectedDevice.setText(name + ":" + address);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "->: " + action);
            //Classic Action Receiver
            if (BluetoothLeService.ACTION_LE_CONNECTION_STATE_CHANGED.equals(action)) {
                notifyCachedDevicesListSetChanged();
                updateSelectedDevice();
            }
        }
    };

    private void dialogShowSelected() {
        dialog = new Dialog(this.getContext(), R.style.ActionSheetDialogStyle);
        inflate = LayoutInflater.from(this.getContext()).inflate(R.layout.dialog_layout, null);
        wifiManager = (TextView) inflate.findViewById(R.id.wifiManager);
        wifiManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "wifiManager Click");
                switchToWiFiManagerFragment();
                dialog.dismiss();
            }
        });
        dialog.setContentView(inflate);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.y = 20;
        dialogWindow.setAttributes(lp);
        dialog.show();
    }

    private void switchToWiFiManagerFragment() {
        getFragmentManager().beginTransaction().replace(R.id.content_main, new WiFiManagerFragment()).commit();
    }

}
