package com.mx.dxinl.gzmtrmap.Structs;

import java.util.HashMap;
import java.util.List;

/**
 * Created by dxinl on 2015/12/19.
 */
public class Node {
	public final String name;
	public final int x;
	public final int y;
	public List<Line> lines;
	public String color;
	public List<Node> neighbors;
	public List<Integer> distances;
	public HashMap<Node, Integer> neighborsDist;

	public Node(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = y;
	}
}
