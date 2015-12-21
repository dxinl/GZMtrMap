package com.mx.dxinl.gzmtrmap.Utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mx.dxinl.gzmtrmap.Structs.Line;
import com.mx.dxinl.gzmtrmap.Structs.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dxinl on 2015/12/19.
 */
public class DbUtils {
	public enum MAX_OR_MIN {
		MAX, MIN
	}
	public static final String SELECT_MAX = "max(%s)";
	public static final String SELECT_MIN = "min(%s)";

	public static List<Node> getNodes(SQLiteDatabase db) {
		List<Node> nodes = new ArrayList<>();
		String sql = "SELECT name, x, y FROM node";
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			String name = cursor.getString(cursor.getColumnIndex("name"));
			int x = cursor.getInt(cursor.getColumnIndex("x"));
			int y = cursor.getInt(cursor.getColumnIndex("y"));
			Node node = new Node(name, x, y);
			nodes.add(node);
		}
		cursor.close();
		return nodes;
	}

	public static int getMaxOrMinValue(SQLiteDatabase db, String tableName, String columns, MAX_OR_MIN type) {
		String sql = "SELECT %s AS %s FROM " + tableName;
		if (type == MAX_OR_MIN.MAX) {
			sql = String.format(sql, String.format(SELECT_MAX, columns), columns);
		} else {
			sql = String.format(sql, String.format(SELECT_MIN, columns), columns);
		}
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();
		int value = cursor.getInt(cursor.getColumnIndex(columns));
		cursor.close();
		return value;
	}

	public static HashMap<String, Integer> getNeighbors(SQLiteDatabase db, String nodeName) {
		HashMap<String, Integer> neighbors = new HashMap<>();
		String sql = String.format("SELECT neighbor, dist FROM Neighbor WHERE name = \"%s\"", nodeName);
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			String name = cursor.getString(cursor.getColumnIndex("neighbor"));
			int dist = cursor.getInt(cursor.getColumnIndex("dist"));
			neighbors.put(name, dist);
		}
		cursor.close();
		return neighbors;
	}

	public static List<String> getLineNames(SQLiteDatabase db, String nodeName) {
		List<String> lineNames = new ArrayList<>();
		String sql = String.format("SELECT line FROM LineNode WHERE node = \"%s\"", nodeName);
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			lineNames.add(cursor.getString(cursor.getColumnIndex("line")));
		}
		cursor.close();
		return lineNames;
	}

	public static List<Line> getLines(SQLiteDatabase db) {
		List<Line> lines = new ArrayList<>();
		String sql = "SELECT name, color FROM Line";
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			String name = cursor.getString(cursor.getColumnIndex("name"));
			String color = cursor.getString(cursor.getColumnIndex("color"));
			Line line = new Line(name, color);
			lines.add(line);
		}
		cursor.close();
		return lines;
	}

	public static String getColorName(SQLiteDatabase db, String lineName) {
		String sql = String.format("SELECT color FROM Line WHERE name = \"%s\"", lineName);
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();
		String color = cursor.getString(cursor.getColumnIndex("color"));
		cursor.close();
		return color;
	}
}
