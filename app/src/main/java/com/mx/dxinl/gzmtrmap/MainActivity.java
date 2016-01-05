package com.mx.dxinl.gzmtrmap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mx.dxinl.gzmtrmap.Structs.Line;
import com.mx.dxinl.gzmtrmap.Structs.Node;
import com.mx.dxinl.gzmtrmap.Utils.AssetsDatabaseHelper;
import com.mx.dxinl.gzmtrmap.Utils.DbUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ChoseNodeListener {
	private static final String TAG = "GZMtrMap";
	private static final String DB_NAME = "mtr.db";
	private static final int MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 48;

	private MtrView mtr;
	private TextView start;
	private TextView end;

	private String startNodeName, endNodeName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (Build.VERSION.SDK_INT >= 23) {
			int checkReadExternal = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
			int checkWriteExternal = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (checkReadExternal == PackageManager.PERMISSION_DENIED
					|| checkWriteExternal == PackageManager.PERMISSION_DENIED) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
						MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
			} else {
				init();
			}
		} else {
			init();
		}

	}

	private void init() {
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
				} else if (endNodeName.length() == 0) {
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

		new DBProcessTask().execute();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE:
				if (grantResults[0] == PackageManager.PERMISSION_DENIED
						|| grantResults[1] == PackageManager.PERMISSION_DENIED) {
					new CloseAppTask().execute();
				} else {
					init();
				}
				break;
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

	public class DBProcessTask extends AsyncTask<Object, Object, List<Node>> {
		private int maxMapCoordinate;
		private AlertDialog dialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new AlertDialog.Builder(MainActivity.this)
					.setIcon(R.mipmap.ic_launcher)
					.setTitle(R.string.tips)
					.setMessage(R.string.loading)
					.setCancelable(false)
					.create();
			dialog.show();
		}

		@Override
		protected List<Node> doInBackground(Object... params) {
			SQLiteDatabase db = AssetsDatabaseHelper.openDatabase(MainActivity.this, DB_NAME);
			if (db != null) {
				List<Node> nodes = DbUtils.getNodes(db);
				int maxX = DbUtils.getMaxOrMinValue(db, "node", "x", DbUtils.MAX_OR_MIN.MAX);
				int minX = DbUtils.getMaxOrMinValue(db, "node", "x", DbUtils.MAX_OR_MIN.MIN);
				int maxY = DbUtils.getMaxOrMinValue(db, "node", "y", DbUtils.MAX_OR_MIN.MAX);
				int minY = DbUtils.getMaxOrMinValue(db, "node", "y", DbUtils.MAX_OR_MIN.MIN);
				maxMapCoordinate = Math.max(Math.max(maxX, maxY), Math.max(Math.abs(minX), Math.abs(minY)));

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

				return nodes;
			}

			return null;
		}

		@Override
		protected void onPostExecute(List<Node> nodes) {
			super.onPostExecute(nodes);
			dialog.dismiss();
			if (nodes != null) {
				mtr.setNodes(nodes, maxMapCoordinate);
			} else {
				Toast.makeText(MainActivity.this, getString(R.string.load_failed), Toast.LENGTH_SHORT).show();
			}
		}
	}

	public class CloseAppTask extends AsyncTask<Object, Integer, Object> {
		private AlertDialog dialog;
		String msg = getString(R.string.no_permission) + getString(R.string.close_app);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new AlertDialog.Builder(MainActivity.this).create();
			dialog.setCancelable(false);
			dialog.setTitle(getString(R.string.close));
			dialog.setMessage(String.format(msg, 3));
			dialog.show();
		}

		@Override
		protected Object doInBackground(Object[] params) {
			try {
				Thread.sleep(1000);
				publishProgress(2);
				Thread.sleep(1000);
				publishProgress(1);
				Thread.sleep(1000);
				publishProgress(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer[] values) {
			super.onProgressUpdate(values);
			dialog.setMessage(String.format(msg, values[0]));
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);
			dialog.dismiss();
			finish();
		}
	}
}
