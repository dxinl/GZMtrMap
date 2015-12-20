package com.mx.dxinl.gzmtrmap;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mx.dxinl.gzmtrmap.Structs.Line;
import com.mx.dxinl.gzmtrmap.Structs.Node;
import com.mx.dxinl.gzmtrmap.Utils.AssetDatabaseOpenHelper;
import com.mx.dxinl.gzmtrmap.Utils.DbUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "GZMtrMap";
	private static final String VERSION = "version";
	private static final String SYSTEM_DB_PATH1 = "/data/data/%s/database/";
	private static final String DB_NAME = "mtr.db";

	private boolean initialize = true;

	private MtrView mtr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mtr = (MtrView) findViewById(R.id.mtr);

		Map<String, Integer> colorMap = new HashMap<>();
		colorMap.put("blue", R.color.blue);
		colorMap.put("indigo", R.color.indigo);
		colorMap.put("cyan", R.color.cyan);
		colorMap.put("teal", R.color.teal);
		colorMap.put("lightgreen", R.color.lightgreen);
		colorMap.put("lime", R.color.lime);
		colorMap.put("brown", R.color.brown);
		colorMap.put("yellow", R.color.yellow);
		colorMap.put("bluegrey", R.color.bluegrey);
		colorMap.put("grey", R.color.grey);
		colorMap.put("red", R.color.red);
		colorMap.put("black", R.color.black);
		mtr.setColorMap(colorMap);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!initialize) {
			return;
		}

		initialize = false;
		SharedPreferences sp = getSharedPreferences(TAG, MODE_PRIVATE);
		String version = sp.getString(VERSION, "0.0");
		String versionName;
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionName = pInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			versionName = "unknown";
		}

		SQLiteDatabase db = null;
		if (!version.equals(versionName)) {
			AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(this, DB_NAME);
			db = dbHelper.openDatabase();
		}

		if (db != null) {
			List<Node> nodes = DbUtils.getNodes(db);
			int maxX = DbUtils.getMaxOrMinValue(db, "node", "x", DbUtils.MAX_OR_MIN.MAX);
			int minX = DbUtils.getMaxOrMinValue(db, "node", "x", DbUtils.MAX_OR_MIN.MIN);
			int maxY = DbUtils.getMaxOrMinValue(db, "node", "y", DbUtils.MAX_OR_MIN.MAX);
			int minY = DbUtils.getMaxOrMinValue(db, "node", "y", DbUtils.MAX_OR_MIN.MIN);
			int maxMapCoordinate = Math.max(Math.max(maxX, maxY), Math.max(Math.abs(minX), Math.abs(minY)));

			List<Line> lines = DbUtils.getLines(db);
			for (Node node : nodes) {
				List<String> lineNames = DbUtils.getLineNames(db, node.name);
				List<Line> nodeLines = new ArrayList<>();
				for (String lineName : lineNames) {
					for (Line line : lines) {
						if (line.name.equals(lineName)) {
							nodeLines.add(line);
							break;
						}
					}
				}
				node.lines = nodeLines;
				if (lineNames.size() == 1) {
					node.color = DbUtils.getColorName(db, lineNames.toArray()[0].toString());
				} else {
					node.color = "black";
				}
				List<String> neighborNames = DbUtils.getNeighbors(db, node.name);
				List<Node> neighbors = new ArrayList<>();
				for (String neighborName : neighborNames) {
					for (Node tmpNode : nodes) {
						if (tmpNode.name.equals(neighborName)) {
							neighbors.add(tmpNode);
							break;
						}
					}
				}
				node.neighbors = neighbors;
			}
			mtr.setNodes(nodes, maxMapCoordinate);
		}
	}
}
