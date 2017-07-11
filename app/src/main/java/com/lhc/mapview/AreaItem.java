package com.lhc.mapview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;

import java.util.Arrays;

/**
 * 作者：lhc
 * 时间：2017/7/11.
 */

public class AreaItem {
    /**
     * 区域名称
     */
    private String name;
    /**
     * 区域路径
     */
    private Path path;

    private String detailItemId;

    private Region region;

    public AreaItem(Path path) {
        this.path = path;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = "";
            this.detailItemId = "";

        } else {
            String[] strs = name.split("\\|");
            String[] result = Arrays.copyOf(strs, 2);
            this.name = result[0];
            this.detailItemId = result[1];
        }
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public String getDetailItemId() {
        return detailItemId;
    }

    public void draw(Canvas canvas, Paint paint, boolean isSelect) {
        if (isSelect) {
            drawItemFill(canvas, paint);
            drawItemStroke(canvas, paint);
        } else {
            drawItemStroke(canvas, paint);
        }
    }

    private void drawItemFill(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FF6347"));
        paint.setShadowLayer(2, 0, 0, Color.GRAY);
        paint.setStrokeWidth(1);
        canvas.drawPath(path, paint);
    }

    private void drawItemStroke(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#1E90FF"));
        paint.setStrokeWidth(1);
        paint.clearShadowLayer();
        canvas.drawPath(path, paint);
    }

    /**
     * 判断该区域是否点击
     *
     * @param x 点击的x坐标
     * @param y 点击的y坐标
     * @return
     */
    public boolean isTouch(int x, int y) {
        if (region == null) {
            RectF rectF = new RectF();
            path.computeBounds(rectF, true);
            region = new Region();
            region.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
        }
        return region.contains(x, y);
    }

}
