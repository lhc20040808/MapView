# 通过svg绘制的地图


## 效果图

![mapview](https://raw.githubusercontent.com/lhc20040808/Pictures/master/res/图片/map_view_gif.gif)



## 实现思路

由于涉及交互，仅仅绘制SVG图肯定是不行的。

1、获取SVG资源

2、通过[工具](http://inloop.github.io/svg2android/)将其转成VectorDrawable

2.1、为了实现点击显示名称或跳转到省市级地图，我自己补充了一下地区名称和后续资源的数据。在VectorDrawable的name属性中加入地区名称和对应省市级资源的id，通过|分割。

3、解析VectorDrawable，通过工具类将pathData转成Path对象

4、适配控件大小，对初始地图进行缩放

5、绘制Path



至于交互并不是SVG绘制的重点，就不在实现思路里面赘述。



## 未解决的问题

//TODO 居中绘制在每个省市的名字。通过path获取rect，由于部分path比如内蒙古的path只占矩形的右边区域，最终绘制名称时会偏移出这个区域。暂时还没想到解决方案。



//TODO 地区选择后的过渡动画



//TODO 解决onScroll阈值所带来拖动初期不流畅的感觉

## 开发中排过的坑

### 文字绘制的居中公式

​	由于文字绘制的纵坐标从BaseLine开始，如果不对想要绘制文字的点坐标进行偏移，最终绘制出的文字得不到想要的效果。

​	公式：
$$
最终绘制纵坐标Y =  绘制纵坐标Y + (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
$$
绘制纵坐标是希望文字居中的点。



### 手势监控

通过`GestureDetectorCompat`进行单点手势的监控，通过`ScaleGestureDetector`对多点进行监控

在测试过程中发现多点手势后很容易触发一次单点（毕竟没办法做到两个手指同时挪开）。这里的解决方案是设置一个标志位不消费`onScale`后的首个`onScroll`。

单点触控由于存在点击事件，每次`onScroll`之前必然回调一次`onDown`，改用`onSingleTapConfirmed`来监听点击事件。

但在实际使用过程中，onScroll因为有个阈值`mTouchSlopSquare`，在拖动初始时有一种稍不流畅的感觉。

### 缩放体验

初始时地图根据控件大小进行缩放并加入一个初始内间距。

防止地图移动时完全移出屏幕，加入了边界检测。