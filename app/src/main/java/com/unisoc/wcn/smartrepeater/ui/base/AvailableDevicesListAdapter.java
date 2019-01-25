package com.unisoc.wcn.smartrepeater.ui.base;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.unisoc.wcn.smartrepeater.R;
import com.unisoc.wcn.smartrepeater.data.BleDevice;
import com.unisoc.wcn.smartrepeater.data.Utils;

import java.util.List;

public class AvailableDevicesListAdapter extends RecyclerView.Adapter<AvailableDevicesListAdapter.MyHolder> {
    private static final String TAG = "AvailableDevicesListAdapter";
    private Context mContext;
    private List<BleDevice> mAvailableDevices;

    public Utils.OnAvailableDeviceListener mOnAvailableDeviceListener;

    public AvailableDevicesListAdapter(Context context, List<BleDevice> datas) {
        super();
        this.mContext = context;
        this.mAvailableDevices = datas;
    }

    @Override
    public int getItemCount() {
        return mAvailableDevices.size();
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position) {
        // TODO Auto-generated method stub
        BleDevice device = mAvailableDevices.get(position);
        holder.textDeviceName.setText(device.getName());
        holder.textDeviceAddress.setText(device.getAddress());
        holder.textDeviceRssi.setText(device.getRssi() + "");
        holder.setPosition(position);
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            BleDevice device = mAvailableDevices.get(position);
            for (int i = 0; i < payloads.size(); i++) {
                switch ((Integer) payloads.get(i)) {
                    case BleDevice.UP_NAME:
                        holder.textDeviceName.setText(device.getName());
                        break;
                    case BleDevice.UP_RSSI:
                        holder.textDeviceRssi.setText(device.getRssi() + "");
                        break;
                    case BleDevice.UP_NAME_RSSI:
                        holder.textDeviceName.setText(device.getName());
                        holder.textDeviceRssi.setText(device.getRssi() + "");
                        break;
                }
            }
        }
    }

    public void setmOnAvailableDeviceListener(Utils.OnAvailableDeviceListener listener) {
        this.mOnAvailableDeviceListener = listener;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup arg0, int arg1) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.available_device, null);
        final MyHolder holder = new MyHolder(view);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnAvailableDeviceListener != null) {
                    BleDevice device = mAvailableDevices.get(holder.position);
                    mOnAvailableDeviceListener.onClick(device);
                }
            }
        });
        return holder;
    }

    class MyHolder extends RecyclerView.ViewHolder {
        TextView textDeviceName;
        TextView textDeviceAddress;
        TextView textDeviceRssi;
        int position = -1;

        public MyHolder(View view) {
            super(view);
            textDeviceName = view.findViewById(R.id.device_name);
            textDeviceAddress = view.findViewById(R.id.device_address);
            textDeviceRssi = view.findViewById(R.id.device_rssi);
        }

        public void setPosition(int position) {
            this.position = position;
        }

    }
}