package com.mx.dxinl.gzmtrmap.Utils;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by DengXinliang on 2016/1/5.
 */
public class ScreenUtils {
	public static int getScreenWidth(Context context) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.widthPixels;
	}

	public static int getScreenHeight(Context context) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return metrics.heightPixels;
	}
}
