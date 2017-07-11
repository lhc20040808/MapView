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
import android.util.Log;
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
    private int translateX;
    private int translateY;
    private RectF mapRectF;
    private Paint.FontMetrics fontMetrics;
    private int oldDis;
    private int newDis;

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
        new XmlParseTask(this).execute();

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                scale = scaleGestureDetector.getScaleFactor();
                Log.d(TAG, "scale---->" + scale);
                if (scale < 1) {
                    scale = 1;
                }
                if (scale > 4) {
                    scale = 4;
                }
                postInvalidate();
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                Log.d(TAG, "focusX----->" + scaleGestureDetector.getFocusX());
                Log.d(TAG, "focusY----->" + scaleGestureDetector.getFocusY());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

            }
        });

        gestureDetectorCompat = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
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
                    postInvalidate();
                }
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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

    private void drawSelectName(Canvas canvas) {
        if (selectItem != null) {
            canvas.save();
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText(selectItem.getName(), getWidth() / 2, 100 + (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom, paint);//TODO 总结文字居中公式
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

    private class XmlParseTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<MapView> mapViewWeakReference;
        private Resources res;

        public XmlParseTask(MapView mapView) {
            this.mapViewWeakReference = new WeakReference<MapView>(mapView);
            this.res = getResources();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            long time = System.currentTimeMillis();
            InputStream inputStream = null;
            try {
                inputStream = res.openRawResource(R.raw.china_map);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                int event = parser.getEventType();
                AreaItem item = null;
                SvgPathParser svgPathParser = new SvgPathParser();
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

                Log.d(TAG, "耗时：" + (System.currentTimeMillis() - time));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mapViewWeakReference.get() != null) {
                translateY = (int) (getHeight() / 2 - mapRectF.height() / 2);
                translateX = (int) (getWidth() / 2 - mapRectF.width() / 2);
                mapViewWeakReference.get().postInvalidate();
            }
        }
    }

}
