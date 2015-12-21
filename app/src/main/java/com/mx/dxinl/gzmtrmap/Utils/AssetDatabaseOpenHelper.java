package com.mx.dxinl.gzmtrmap.Utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetDatabaseOpenHelper {
	private final Context context;
	private final String dir;
	private final String DB_NAME;

	public AssetDatabaseOpenHelper(Context context, String dir, String dbName) {
		this.context = context;
		this.dir = dir;
		this.DB_NAME = dbName;
	}

	public SQLiteDatabase openDatabase() {
		String dbDir = Environment.getExternalStorageDirectory().getPath();
		String separator = "/";
		String[] paths = dir.split(separator);
		for (String path : paths) {
			dbDir = dbDir + separator + path;
			File file = new File(dbDir);
			if (!file.exists() || !file.isDirectory()) {
				if (!file.mkdir()) {
					return null;
				}
			}
		}

		File dbFile = new File(dbDir + separator + DB_NAME);
		try {
			if (!dbFile.exists() || !dbFile.isFile()) {
				if (!dbFile.createNewFile()) {
					throw new IOException("Create File Failed.");
				}
			}
			copyDatabase(dbFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error creating source database", e);
		}

		return SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
	}

	private void copyDatabase(File dbFile) throws IOException {
		InputStream is = context.getAssets().open(DB_NAME);
		OutputStream os = new FileOutputStream(dbFile);

		byte[] buffer = new byte[1024];
		int length;
		while ((length = is.read(buffer)) > 0) {
			os.write(buffer, 0, length);
		}

		os.flush();
		os.close();
		is.close();
	}

}
