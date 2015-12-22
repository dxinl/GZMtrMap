package com.mx.dxinl.gzmtrmap;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mx.dxinl.gzmtrmap.Structs.Line;
import com.mx.dxinl.gzmtrmap.Structs.Node;
import com.mx.dxinl.gzmtrmap.Utils.AssetDatabaseOpenHelper;
import com.mx.dxinl.gzmtrmap.Utils.DbUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ChoseNodeListener {
	private static final String TAG = "GZMtrMap";
	private static final String VERSION = "version";
	private static final String DB_DIR = "/databases";
	private static final String DB_NAME = "mtr.db";
	private static final String SEPARATOR = "/";

	private boolean initialize = true;

	private MtrView mtr;
	private TextView start;
	private TextView end;

	private String startNodeName, endNodeName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mtr = (MtrView) findViewById(R.id.mtr);
		start = (EditText) findViewById(R.id.start);
		end = (EditText) findViewById(R.id.end);
		Button findRouteBtn = (Button) findViewById(R.id.find_route);
		findRouteBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startNodeName = start.getText().toString();
				endNodeName = end.getText().toString();
				if (startNodeName == null || startNodeName.length() == 0) {
					Toast.makeText(MainActivity.this,
							String.format(getString(R.string.cannot_be_blank), getString(R.string.start)), Toast.LENGTH_SHORT).show();
				} else if (endNodeName == null || endNodeName.length() == 0) {
					Toast.makeText(MainActivity.this,
							String.format(getString(R.string.cannot_be_blank), getString(R.string.end)), Toast.LENGTH_SHORT).show();
				} else if (startNodeName.equals(endNodeName)) {
					Toast.makeText(MainActivity.this, getString(R.string.start_equals_end), Toast.LENGTH_SHORT).show();
				} else {
					Log.e("path", mtr.findRoute(startNodeName, endNodeName));
				}
			}
		});

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
		mtr.setChoseNodeListener(this);
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

		String dir = getPackageName() + DB_DIR;
		SQLiteDatabase db = null;
		if (!version.equals(versionName)) {
			AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(this, dir, DB_NAME);
			db = dbHelper.openDatabase();

			SharedPreferences.Editor editor = sp.edit();
			editor.putString(VERSION, versionName).apply();
		} else {
			String dbDir = Environment.getExternalStorageDirectory().getPath() + SEPARATOR + dir + SEPARATOR + DB_NAME;
			File file = new File(dbDir);
			if (!file.exists() || !file.isFile()) {
				AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(this, dir, DB_NAME);
				db = dbHelper.openDatabase();
			} else {
				db = SQLiteDatabase.openDatabase(dbDir, null, SQLiteDatabase.OPEN_READONLY);
			}
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
				HashMap<String, Integer> neighborsMap = DbUtils.getNeighbors(db, node.name);
				HashMap<Node, Integer> neighborsDist = new HashMap<>();
				for (String neighborName : neighborsMap.keySet()) {
					for (Node tmpNode : nodes) {
						if (tmpNode.name.equals(neighborName)) {
							neighborsDist.put(tmpNode, neighborsMap.get(neighborName));
							break;
						}
					}
				}
				node.neighborsDist = neighborsDist;
			}
			mtr.setNodes(nodes, maxMapCoordinate);
		}
	}

	@Override
	public void setStartNode(String name) {
		start.setText(name);
	}

	@Override
	public void setEndNode(String name) {
		end.setText(name);
	}
}
