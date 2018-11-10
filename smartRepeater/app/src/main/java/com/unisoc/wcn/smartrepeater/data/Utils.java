package com.unisoc.wcn.smartrepeater.data;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.unisoc.wcn.smartrepeater.R;

public class Utils {
    public interface OnAvailableDeviceListener {
        void onClick(BleDevice device);
    }

    public static byte[] strToByteArray(String str) {
        if (str == null) {
            return null;
        }
        byte[] byteArray = str.getBytes();
        return byteArray;
    }

    public static byte[] int2bytes_two(int num) {
        byte[] result = new byte[2];
        result[1] = (byte) ((num >>> 8) & 0xff);
        result[0] = (byte) ((num >>> 0) & 0xff);
        return result;
    }

    public static int bytes2int_two(byte[] bytes) {
        int result = 0;
        if (bytes.length == 2) {
            int a = (bytes[1] & 0xff) << 8;
            int b = (bytes[0] & 0xff);
            result = a | b;
        }
        return result;
    }

    public static String byteArrayToStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        String str = new String(byteArray);
        return str;
    }

    public static byte[] hexStrToByteArray(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte) Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }

    public static String ByteArrayToMacStr(byte[] macBytes) {
        if (macBytes == null) {
            return null;
        }
        if (macBytes.length == 0) {
            return "";
        }
        String addressStr = "";
        for (int i = 0; i < macBytes.length; i++) {
            String sTemp = Integer.toHexString(0xFF & macBytes[i]);
            addressStr = addressStr + sTemp + ":";
        }
        addressStr = addressStr.substring(0, addressStr.lastIndexOf(":"));
        return addressStr;
    }

    public static String printHexString(byte[] b) {
        String mess = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            mess = mess + " " + hex.toUpperCase();
        }
        return mess;
    }

    public static Dialog createLoadingDialog(Context context, String msg) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_loading, null);
        LinearLayout layout = (LinearLayout) v
                .findViewById(R.id.dialog_loading_view);
        TextView tipTextView = (TextView) v.findViewById(R.id.tipTextView);
        tipTextView.setText(msg);

        Dialog loadingDialog = new Dialog(context, R.style.MyDialogStyle);
        loadingDialog.setCancelable(true);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        Window window = loadingDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setGravity(Gravity.CENTER);
        window.setAttributes(lp);
        window.setWindowAnimations(R.style.PopWindowAnimStyle);
        loadingDialog.show();

        return loadingDialog;
    }

    public static void closeDialog(Dialog mDialogUtils) {
        if (mDialogUtils != null && mDialogUtils.isShowing()) {
            mDialogUtils.dismiss();
        }
    }
}
