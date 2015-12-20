package com.mx.dxinl.gzmtrmap;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.mx.dxinl.gzmtrmap.Structs.Line;
import com.mx.dxinl.gzmtrmap.Structs.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by dxinl on 2015/12/19.
 */
public class MtrView extends View {
	// 每个地图坐标单位中包括了几个unit
	private final float numOfUnitInEveryMapCoordinate = 3f;
	private final Paint paint = new Paint();

	private List<Node> nodes;
	private Map<String, Integer> colorMap;
	private int maxMapCoordinate;
	private float unit;
	private float centerX;
	private float centerY;
	private float multiTouchCenterX;
	private float multiTouchCenterY;
	private float startDist;
	private float zoomSize;
	private float startDragX;
	private float startDragY;
	private float dragSizeX;
	private float dragSizeY;
	private float clickX;
	private float clickY;
	private int saveClickX;
	private int saveClickY;
	private Button btn;

	public MtrView(Context context) {
		super(context);
	}

	public MtrView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MtrView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public MtrView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				clickX = event.getX();
				clickY = event.getY();
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				clickX = 0;
				clickY = 0;
				startDist = 0;
				return true;
			case MotionEvent.ACTION_MOVE:
				if (event.getPointerCount() >= 2) {
					int index1 = event.findPointerIndex(event.getPointerId(0));
					int index2 = event.findPointerIndex(event.getPointerId(1));
					float moveDist = getFingerDist(event);
					if (startDist == 0f) {
						startDist = moveDist;
						if (multiTouchCenterX == 0f && multiTouchCenterY == 0f) {
							multiTouchCenterX = (event.getX(index1) + event.getX(index2)) / 2f;
							multiTouchCenterY = (event.getY(index1) + event.getY(index2)) / 2f;
						}
						return true;
					}

					float dist = moveDist - startDist;
					if (Math.abs(dist) > 10) {
						zoomSize += dist;
						if (zoomSize < 0) {
							zoomSize = 0f;
						} else if (zoomSize > unit * 100f) {
							zoomSize = unit * 100f;
						}
						startDist = moveDist;
						clickY = clickX = 0f;
						invalidate();
						return true;
					}
				} else {
					if (zoomSize == 0f) {
						return true;
					}
					if (startDragY == 0f && startDragX == 0f) {
						startDragX = event.getX();
						startDragY = event.getY();
						return true;
					}
					float dragX = event.getX() - startDragX;
					float dragY = event.getY() - startDragY;
					if ((Math.abs(dragX) > 10 || Math.abs(dragY) > 10)) {
						dragSizeX += dragX;
						dragSizeY += dragY;
						startDragX = event.getX();
						startDragY = event.getY();
						clickY = clickX = 0f;
						invalidate();
					}
				}
				return true;
			case MotionEvent.ACTION_UP:
				startDist = 0f;
				startDragX = 0f;
				startDragY = 0f;
				dragSizeX = 0f;
				dragSizeY = 0f;
				if (zoomSize == 0f) {
					multiTouchCenterX = multiTouchCenterY = 0f;
				}
				if (Math.abs(event.getX() - clickX) <= 10 && Math.abs(event.getY() - clickY) <= 10) {
					invalidate();
				} else {
					clickX = clickY = 0;
				}
				return true;
		}
		return super.onTouchEvent(event);
	}

	private float getFingerDist(MotionEvent event) {
		int index1 = event.findPointerIndex(event.getPointerId(0));
		int index2 = event.findPointerIndex(event.getPointerId(1));
		float x1 = event.getX(index1);
		float y1 = event.getY(index1);
		float x2 = event.getX(index2);
		float y2 = event.getY(index2);

		return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (nodes == null) {
			return;
		}

		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		if (clickX != 0f && clickY != 0f) {
			zoomDraw(canvas);
			clickX = clickY = 0f;
			return;
		}

		if (zoomSize == 0f) {
			initDraw(canvas);
		} else if (dragSizeX == 0f && dragSizeY == 0f) {
			zoomDraw(canvas);
		} else {
			dragDraw(canvas);
		}
	}

	private void initDraw(Canvas canvas) {
		unit = Math.min(getWidth(), getHeight()) / 2f / this.maxMapCoordinate / numOfUnitInEveryMapCoordinate;
		centerX = getWidth() / 2f;
		centerY = getHeight() / 2f;

		List<Node> nodeDrewLine = new ArrayList<>();
		for (Node node : nodes) {
			float absoluteX = getAbsoluteXCoordinate(node.x, centerX, unit);
			float absoluteY = getAbsoluteYCoordinate(node.y, centerY, unit);

			drawLine(canvas, node, nodeDrewLine, unit, absoluteX, absoluteY, centerX, centerY);
			nodeDrewLine.add(node);

			drawCircle(canvas, node, absoluteX, absoluteY, unit);

			if (saveClickX == node.x && saveClickY == node.y) {
				drawClickCircle(canvas, unit, absoluteX, absoluteY);
			}

			if (btn != null) {
				btn.setVisibility(GONE);
			}
		}
		nodeDrewLine.clear();
	}

	private void zoomDraw(Canvas canvas) {
		float newUnit = unit + zoomSize / 20f;
		boolean isFoundClick = true;
		if (clickX != 0f && clickY != 0f) {
			isFoundClick = false;
		} else if (btn != null) {
			btn.setVisibility(GONE);
		}

		List<Node> nodeDrewLine = new ArrayList<>();
		for (Node node : nodes) {
			float relativeCenterX = getRelativeCenter(centerX, multiTouchCenterX, newUnit);
			float relativeCenterY = getRelativeCenter(centerY, multiTouchCenterY, newUnit);
			float absoluteX = getAbsoluteXCoordinate(node.x, relativeCenterX, newUnit);
			float absoluteY = getAbsoluteYCoordinate(node.y, relativeCenterY, newUnit);
			// We will not draw the node if it is out of this view,
			if (absoluteX < 0 || absoluteX > getWidth() || absoluteY < 0 || absoluteY > getHeight()) {
				continue;
			}

			drawLine(canvas, node, nodeDrewLine,
					newUnit, absoluteX, absoluteY, relativeCenterX, relativeCenterY);
			nodeDrewLine.add(node);

			drawCircle(canvas, node, absoluteX, absoluteY, newUnit);

			if (!isFoundClick
					&& Math.abs(absoluteX - clickX) < newUnit * 1.5f
					&& Math.abs(absoluteY - clickY) < newUnit * 1.5f) {
				saveClickX = node.x;
				saveClickY = node.y;
				isFoundClick = true;
				if (btn == null) {
					btn = new Button(getContext());
				}
				if (btn.getParent() != null) {
					((ViewGroup) btn.getParent()).removeView(btn);
				}

				btn.setText(node.name);
				btn.setVisibility(VISIBLE);
				if (getParent() instanceof RelativeLayout) {
					RelativeLayout layout = (RelativeLayout) getParent();
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					if (getWidth() - absoluteX < btn.getWidth()) {
						params.leftMargin = getWidth() - btn.getWidth();
					} else {
						params.leftMargin = (int) (absoluteX + newUnit);
					}

					if (getHeight() - absoluteY < btn.getWidth()) {
						params.topMargin = (int) (absoluteY - newUnit) - btn.getHeight();
					} else {
						params.topMargin = (int) (absoluteY + newUnit);
					}
					layout.addView(btn, params);
				}
			}

			if (isFoundClick && saveClickX == node.x && saveClickY == node.y) {
				drawClickCircle(canvas, newUnit, absoluteX, absoluteY);
			}

			drawName(canvas, node, absoluteX, absoluteY, newUnit);
		}
		nodeDrewLine.clear();
		if (!isFoundClick) {
			saveClickY = saveClickX = -maxMapCoordinate;
		}
	}

	private void dragDraw(Canvas canvas) {
		float newUnit = unit + zoomSize / 20f;
		float dragX = dragSizeX / 2f;
		float dragY = dragSizeY / 2f;
		float totalDragX;
		float totalDragY;
		if (dragX > 0) {
			totalDragX = multiTouchCenterX * newUnit - multiTouchCenterX;
		} else {
			totalDragX = (getWidth() - multiTouchCenterX) * newUnit - (getWidth() - multiTouchCenterX);
		}
		if (dragY > 0) {
			totalDragY = multiTouchCenterY * newUnit - multiTouchCenterY;
		} else {
			totalDragY = (getHeight() - multiTouchCenterY) * newUnit - (getHeight() - multiTouchCenterY);
		}

		if (Math.abs(dragX) > totalDragX) {
			dragX = dragX / Math.abs(dragX) * totalDragX;
		}
		if (Math.abs(dragY) > totalDragY) {
			dragY = dragY / Math.abs(dragY) * totalDragY;
		}

		multiTouchCenterX -= dragX / newUnit;
		multiTouchCenterY -= dragY / newUnit;

		List<Node> nodeDrewLine = new ArrayList<>();
		for (Node node : nodes) {
			float relativeCenterX = getRelativeCenter(centerX, multiTouchCenterX, newUnit);
			float relativeCenterY = getRelativeCenter(centerY, multiTouchCenterY, newUnit);
			float absoluteX = getAbsoluteXCoordinate(node.x, relativeCenterX, newUnit);
			float absoluteY = getAbsoluteYCoordinate(node.y, relativeCenterY, newUnit);
			// We will not draw the node if it is out of this view,
			if (absoluteX < 0 || absoluteX > getWidth() || absoluteY < 0 || absoluteY > getHeight()) {
				continue;
			}

			drawLine(canvas, node, nodeDrewLine,
					newUnit, absoluteX, absoluteY, relativeCenterX, relativeCenterY);
			nodeDrewLine.add(node);

			drawCircle(canvas, node, absoluteX, absoluteY, newUnit);

			if (saveClickX == node.x && saveClickY == node.y) {
				drawClickCircle(canvas, newUnit, absoluteX, absoluteY);
			}

			drawName(canvas, node, absoluteX, absoluteY, newUnit);

			if (btn != null) {
				btn.setVisibility(GONE);
			}
		}
		nodeDrewLine.clear();
	}

	private void drawLine(Canvas canvas, Node node, List<Node> nodeDrewLine,
	                      float unit, float absoluteX, float absoluteY, float centerX, float centerY) {
		for (Node neighbor : node.neighbors) {
			if (nodeDrewLine.contains(neighbor)) {
				continue;
			}
			int colorId = getColorId(node, neighbor);
			paint.setColor(getResources().getColor(colorId));
			paint.setStrokeWidth(unit);
			float neighborAbsoluteX = getAbsoluteXCoordinate(neighbor.x, centerX, unit);
			float neighborAbsoluteY = getAbsoluteYCoordinate(neighbor.y, centerY, unit);
			canvas.drawLine(absoluteX, absoluteY, neighborAbsoluteX, neighborAbsoluteY, paint);
		}
	}

	private void drawCircle(Canvas canvas, Node node,
	                        float absoluteX, float absoluteY, float radius) {
		int colorId = getColorId(node.color);
		paint.setStrokeWidth(unit);
		paint.setColor(getResources().getColor(colorId));
		canvas.drawCircle(absoluteX, absoluteY, radius, paint);
	}

	private void drawClickCircle(Canvas canvas, float newUnit, float absoluteX, float absoluteY) {
		paint.setColor(getResources().getColor(R.color.red));
		canvas.drawCircle(absoluteX, absoluteY, 1.25f * newUnit, paint);
	}

	private void drawName(Canvas canvas, Node node, float absoluteX, float absoluteY, float newUnit) {
		if (zoomSize > unit * 30f) {
			paint.setTextSize(newUnit * 2f);
			paint.setColor(getResources().getColor(R.color.black));
			canvas.drawText(node.name, absoluteX + newUnit, absoluteY + newUnit * 3f, paint);
		}
	}

	private float getAbsoluteXCoordinate(int mapCoordinate, float centerX, float unit) {
		return centerX + unit * numOfUnitInEveryMapCoordinate * mapCoordinate;
	}

	private float getAbsoluteYCoordinate(int mapCoordinate, float centerY, float unit) {
		return centerY - unit * numOfUnitInEveryMapCoordinate * mapCoordinate;
	}

	private float getRelativeCenter(float center, float multiTouchCenter, float newUnit) {
		return multiTouchCenter + (center - multiTouchCenter) * newUnit / unit;
	}

	private int getColorId(String colorName) {
		if (colorMap != null) {
			return colorMap.get(colorName);
		}
		return R.color.black;
	}

	private int getColorId(Node node, Node neighbor) {
		for (Line line : node.lines) {
			for (Line neighborLine : neighbor.lines) {
				if (line.name.equals(neighborLine.name)) {
					return getColorId(line.color);
				}
			}
		}
		return R.color.black;
	}

	public void setNodes(List<Node> nodes) {
		maxMapCoordinate = 0;
		for (Node node : nodes) {
			maxMapCoordinate = Math.max(Math.abs(node.x), maxMapCoordinate);
			maxMapCoordinate = Math.max(Math.abs(node.y), maxMapCoordinate);
		}
		setNodes(nodes, maxMapCoordinate);
	}

	public void setNodes(List<Node> nodes, int maxMapCoordinate) {
		this.nodes = nodes;
		this.maxMapCoordinate = maxMapCoordinate;
		this.maxMapCoordinate++;
		saveClickY = saveClickX = -this.maxMapCoordinate;
		invalidate();
	}

	public void setColorMap(Map<String, Integer> colorMap) {
		this.colorMap = colorMap;
	}
}