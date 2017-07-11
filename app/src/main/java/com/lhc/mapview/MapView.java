package com.lhc.mapview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lhc
 * 时间：2017/7/11.
 */

public class MapView extends View {

    private static final String TAG = "LHC";
    private List<AreaItem> areaItems = new ArrayList<>();
    private Paint paint;
    private AreaItem selectItem;
    private float scale = 1f;
    private GestureDetectorCompat gestureDetectorCompat;
    private ScaleGestureDetector scaleGestureDetector;
    private float translateX;
    private float translateY;
    private RectF mapRectF;
    private Paint.FontMetrics fontMetrics;
    private float oldScale;
    private int width;
    private int height;
    private boolean isLoadFinish;
    private boolean hasScale;

    public MapView(Context context) {
        this(context, null);
    }

    public MapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mapRectF = new RectF();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        fontMetrics = paint.getFontMetrics();
        new XmlParseTask(this).execute(R.raw.china_map);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                scale *= (scaleGestureDetector.getCurrentSpan() / oldScale);

                if (scale < 1) {
                    scale = 1;
                }

                if (scale > 2) {
                    scale = 2;
                }
                oldScale = scaleGestureDetector.getCurrentSpan();
                hasScale = true;
                postInvalidate();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                oldScale = scaleGestureDetector.getPreviousSpan();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

            }
        });

        gestureDetectorCompat = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                //onScale执行后如果有单手移动会触发onScroll，屏蔽第一次事件防止地图闪到其他位置
                if (hasScale) {
                    hasScale = !hasScale;
                    return false;
                }

                translateX -= distanceX;
                translateY -= distanceY;

                //边界碰撞检测，地图尺寸小于控件尺寸时不能移出控件，地图尺寸大于控件尺寸时只能挪出1/3
                if (mapRectF.height() * scale < height && mapRectF.width() * scale < width) {
                    //地图比屏幕小
                    if (translateX < 0) {
                        translateX = 0;
                    }

                    if (translateX + mapRectF.width() * scale > width) {
                        translateX = width - mapRectF.width() * scale;
                    }

                    if (translateY < 0) {
                        translateY = 0;
                    }

                    if (translateY + mapRectF.height() * scale > height) {
                        translateY = height - mapRectF.height() * scale;
                    }
                } else if (mapRectF.width() * scale > width || mapRectF.height() * scale > height) {
                    if (translateX + mapRectF.width() * scale * 2 / 3 > width) {
                        translateX = width - mapRectF.width() * scale * 2 / 3;
                    }

                    if (translateX + mapRectF.width() * scale * 1 / 3 < 0) {
                        translateX = -mapRectF.width() * scale * 1 / 3;
                    }
                    if (translateY + mapRectF.height() * scale * 2 / 3 > height) {
                        translateY = height - mapRectF.height() * scale * 2 / 3;
                    }

                    if (translateY + mapRectF.height() * scale * 1 / 3 < 0) {
                        translateY = -mapRectF.height() * scale * 1 / 3;
                    }
                }

                postInvalidate();
                return true;
            }
        });

        gestureDetectorCompat.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                int x = (int) e.getX();
                int y = (int) e.getY();
                AreaItem tmp = null;
                for (AreaItem item : areaItems) {
                    if (item.isTouch((int) ((x - translateX) / scale), (int) ((y - translateY) / scale))) {
                        tmp = item;
                        break;
                    }
                }

                if (tmp != null) {
                    selectItem = tmp;

                    String detailId = selectItem.getDetailItemId();
                    if (detailId != null && !"".equals(detailId)) {
                        int id = getResources().getIdentifier(detailId, "raw", getContext().getPackageName());
                        new XmlParseTask(MapView.this).execute(id);
                    }
                    postInvalidate();
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent motionEvent) {
                return false;
            }
        });

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isLoadFinish) {
            canvas.save();
            canvas.translate(translateX, translateY);
            canvas.scale(scale, scale);
            for (AreaItem item : areaItems) {
                if (item != selectItem) {
                    item.draw(canvas, paint, false);
                }
            }

            if (selectItem != null) {
                selectItem.draw(canvas, paint, true);
            }
            canvas.restore();

            drawSelectName(canvas);
        }
    }

    private void drawSelectName(Canvas canvas) {
        if (selectItem != null) {
            canvas.save();
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText(selectItem.getName(), width / 2, 100 + (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom, paint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int count = event.getPointerCount();
        if (count == 1) {
            return gestureDetectorCompat.onTouchEvent(event);
        } else {
            return scaleGestureDetector.onTouchEvent(event);
        }
    }

    private class XmlParseTask extends AsyncTask<Integer, Void, Void> {

        private WeakReference<MapView> mapViewWeakReference;
        private Resources res;

        public XmlParseTask(MapView mapView) {
            this.mapViewWeakReference = new WeakReference<MapView>(mapView);
            this.res = getResources();
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            InputStream inputStream = null;
            int id = integers[0];
            try {
                inputStream = res.openRawResource(id);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                int event = parser.getEventType();
                AreaItem item = null;
                SvgPathParser svgPathParser = new SvgPathParser();

                mapViewWeakReference.get().areaItems.clear();

                while (event != XmlPullParser.END_DOCUMENT) {
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            if ("path".equals(parser.getName())) {
                                String pathData = parser.getAttributeValue(null, "pathData");
                                String name = parser.getAttributeValue(null, "name");
                                item = new AreaItem(svgPathParser.parsePath(pathData));
                                item.setName(name);
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if ("path".equals(parser.getName())) {
                                mapViewWeakReference.get().areaItems.add(item);
                            }
                            break;
                        default:
                            break;
                    }
                    event = parser.next();
                }

                Path tmpPath = new Path();
                for (AreaItem temp : areaItems) {
                    tmpPath.addPath(temp.getPath());
                }

                tmpPath.computeBounds(mapRectF, true);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mapViewWeakReference.get() != null) {
                mapViewWeakReference.get().selectItem = null;
                translateY = (int) (height / 2 - mapRectF.height() / 2);
                translateX = (int) (width / 2 - mapRectF.width() / 2);
                isLoadFinish = true;
                mapViewWeakReference.get().postInvalidate();
            }
        }
    }

}
