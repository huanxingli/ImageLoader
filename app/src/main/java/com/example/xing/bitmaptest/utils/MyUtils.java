package com.example.xing.bitmaptest.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

/**
 * Created by xiaoxingxing on 2016/8/2.
 */
public class MyUtils {
    public static DisplayMetrics getScreenMetrics(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    public static float dp2px(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }


}
