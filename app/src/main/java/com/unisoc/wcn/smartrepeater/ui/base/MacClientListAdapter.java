package com.unisoc.wcn.smartrepeater.ui.base;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unisoc.wcn.smartrepeater.R;

import java.util.List;

/**
 * Created by clay.zhu on 2018/12/5.
 */

public class MacClientListAdapter extends BaseAdapter {

    private List<String> mCachedClient;
    private LayoutInflater mInflater;

    private static final String TAG = "CachedDevicesListAdapter";

    public MacClientListAdapter(Context context,
                                List<String> availableClient) {
        mInflater = LayoutInflater.from(context);
        mCachedClient = availableClient;
    }

    public int getCount() {
        return mCachedClient.size();
    }

    public Object getItem(int position) {
        return mCachedClient.get(position);
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
                    .inflate(R.layout.cached_client, null);
        }

        if (mCachedClient.size() == 0)
            return vg;

        String clientName = mCachedClient.get(position);

        ((TextView) vg.findViewById(R.id.client_mac)).setText(clientName);
        return vg;
    }
}


