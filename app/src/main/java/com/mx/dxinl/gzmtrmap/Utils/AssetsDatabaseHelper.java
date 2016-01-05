package com.mx.dxinl.gzmtrmap.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dxinl on 2016/1/4.
 */
public class AssetsDatabaseHelper {
	private static final String TAG = AssetsDatabaseHelper.class.getSimpleName();
	private static final String DB_FOLDER = "databases";
	private static final String SEPARATOR = "/";
	private static final String VERSION = "version";
	private static SQLiteDatabase DB = null;

	public static boolean copyDBFromAssets(Context context, String dbName) throws IOException {
		String dir = context.getFilesDir().getPath();

		// make dirs
		String path = dir + SEPARATOR + DB_FOLDER;
		File file = new File(path);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				return false;
			}
		}

		path = path + SEPARATOR + dbName;
		file = new File(path);
		if (!file.exists() || !file.isFile()) {
			if (!file.createNewFile()) {
				return false;
			}
		}

		InputStream is = context.getAssets().open(dbName);
		OutputStream os = new FileOutputStream(file);
		int length;
		byte[] buffer = new byte[1024];
		while ((length = is.read(buffer)) > 0) {
			os.write(buffer, 0, length);
		}
		os.flush();
		os.close();
		is.close();

		return true;
	}

	public static SQLiteDatabase openDatabase(Context context, String dbName) {
		if (DB == null) {
			try {
				String dir = context.getFilesDir().getPath();
				String path = dir + SEPARATOR + DB_FOLDER + SEPARATOR + dbName;
				File file = new File(path);
				if (!file.exists() || !file.isFile()) {
					if (!copyDBFromAssets(context, dbName)) {
						return null;
					}
				} else {
					String pkgName = context.getPackageName();
					SharedPreferences sp = context.getSharedPreferences(pkgName, Context.MODE_PRIVATE);
					PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
					String versionName = pkgInfo.versionName;
					if (!sp.getString(VERSION, VERSION).equals(versionName)) {
						if (!copyDBFromAssets(context, dbName)) {
							return null;
						}
						sp.edit().putString(VERSION, versionName).apply();
					}
				}

				DB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
			} catch (IOException | PackageManager.NameNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		}
		return DB;
	}
}
