package com.mx.dxinl.gzmtrmap.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class AssetDatabaseOpenHelper {
	private final String DB_NAME;
	private final Context context;

	public AssetDatabaseOpenHelper(Context context, String dbName) {
		this.context = context;
		this.DB_NAME = dbName;
	}

	public SQLiteDatabase openDatabase() {
		String dir = Environment.getExternalStorageDirectory().getPath() + "/" + context.getPackageName() + "/databases";
		File dirFile = new File(dir);
		if (!dirFile.exists() || !dirFile.isDirectory() && !dirFile.mkdirs()) {
			return null;
		}
		File dbFile = new File(dir + "/" + DB_NAME);

		try {
			copyDatabase(dbFile);
		} catch (IOException e) {
			throw new RuntimeException("Error creating source database", e);
		}

		return SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
	}

	private void copyDatabase(File dbFile) throws IOException {
		InputStream is = context.getAssets().open(DB_NAME);
		if (!dbFile.exists() || !dbFile.isFile() && dbFile.createNewFile()) {
			throw new IOException("Create File Failed.");
		}
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
