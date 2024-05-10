package com.ticonsys.screencapture.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PMSharedPrefUtils {
	public final static String USER_IP = "USER_IP";
	public final static String HOME_API = "HOME_API";

	public static void setIP(Context context, String key, String ip) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(key, ip);
		editor.commit();
	}
	public static String getIP(Context context, String key) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(key, "");
	}

	public static void setHomeApi(Context context, String key, String ip) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(key, ip);
		editor.commit();
	}
	public static String getHomeApi(Context context, String key) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(key, "");
	}
}
