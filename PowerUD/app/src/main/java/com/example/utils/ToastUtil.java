package com.example.utils;

import android.content.Context;
import android.widget.Toast;


public class ToastUtil {
    public static Toast toast;
    public static void showMsg(Context context, String msg){
        if(toast == null){
            toast = Toast.makeText(context,msg,Toast.LENGTH_SHORT);
        }
        else {
            toast.cancel();
            toast.setText(msg);
        }
        toast.show();
    }
}