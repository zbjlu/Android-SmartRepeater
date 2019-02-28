package com.unisoc.wcn.smartrepeater.ui.base;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unisoc.wcn.smartrepeater.R;
import com.unisoc.wcn.smartrepeater.data.BleDevice;

import java.util.List;

public class CachedDevicesListAdapter extends BaseAdapter {
    private List<BleDevice> mCachedDevices;
    private LayoutInflater mInflater;

    private static final String TAG = "CachedDevicesListAdapter";

    public CachedDevicesListAdapter(Context context,
                                    List<BleDevice> availableDevices) {
        mInflater = LayoutInflater.from(context);
        mCachedDevices = availableDevices;
    }

    public int getCount() {
        return mCachedDevices.size();
    }

    public Object getItem(int position) {
        return mCachedDevices.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView;
        } else {
            vg = (ViewGroup) mInflater
                    .inflate(R.layout.cached_device, null);
        }

        if (mCachedDevices.size() == 0)
            return vg;

        BleDevice device = mCachedDevices.get(position);

        ((TextView) vg.findViewById(R.id.cdevice_name)).setText(device.getName());
        ((TextView) vg.findViewById(R.id.cdevice_address)).setText(device.getAddress());
        ((TextView) vg.findViewById(R.id.cdevice_status)).setText(device.getStatus());
        return vg;
    }
}