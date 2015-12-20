package com.mx.dxinl.gzmtrmap.Structs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	public Node(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = y;
	}
}
