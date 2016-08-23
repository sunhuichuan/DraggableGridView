package com.andyken.draggablegridview.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Created by andyken on 16/3/22.
 */
public class DraggableGridView extends FrameLayout implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "DraggableGridView";

    //当ViewGroup被touchDown时的坐标;用于长按事件发生时，判断是否挪动View
    private int touchDownX = -1, touchDownY = -1;
    private int draggedIndex = -1, lastX = -1, lastY = -1, lastTargetIndex = -1;
    private int xPadding, yPadding;//the x-axis and y-axis padding of the item
    private int itemWidth, itemHeight, colCount;
    //所有子View的index？
    private ArrayList<Integer> newPositions = new ArrayList<Integer>();
    private static int ANIM_DURATION = 150;

    private AdapterView.OnItemClickListener onItemClickListener;
    private OnRearrangeListener onRearrangeListener;


    //大于此值认为是滑动
    final int touchSlop;

    //indicatorView的index
    private int mIndicatorIndex;
    //指示移动的View落点的View
    private View mIndicatorView;

    private List<View> childViewList = new ArrayList<>();

    ScrollView parentScrollView;
    //父View 的 坐标
    private Rect parentRect = new Rect();
    //当距离父View底部距离在此返回，向上滚动
    private int TOUCH_SPACE = 0;


    public DraggableGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        TOUCH_SPACE = dip2px(context,150f);
        init();
    }

    private void init() {

        initAttributes();
        initData();
        initEventListener();
    }

    private void initAttributes() {

        Context context = getContext();
        itemWidth = dip2px(context,60);
        itemHeight = dip2px(context,34);
        colCount = 4;
        yPadding = dip2px(context,19);
    }

    private void initData() {
        setChildrenDrawingOrderEnabled(true);
    }

    private void initEventListener() {
        super.setOnClickListener(this);
        setOnTouchListener(this);
        setOnLongClickListener(this);
    }

    public void addIndicatorView(View child) {
        super.addView(child);
        mIndicatorView = child;
    }


    //把指示View移动到指定index
    void moveIndicatorViewToIndex(int index) {
        Log.i(TAG,"moveIndicatorViewToIndex --> "+index);
        if (mIndicatorView == null) {
            //没有indicator,啥也不做
            return;
        }
        if (index == -1 || mIndicatorIndex == index) {
            //index无效，或者index和当前位置一样，就保持当前位置啥也不做
            return;
        }

        layoutViewToIndex(mIndicatorView, index);

        mIndicatorIndex = index;
    }

    public void addChildView(View child) {
        super.addView(child);
        childViewList.add(child);
        newPositions.add(-1);
    }

    //删除View
    public void removeChildViewAt(int index) {
        View remove = childViewList.remove(index);
        super.removeView(remove);
        newPositions.remove(index);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (parentScrollView == null){
            ViewParent parent = getParent();
            if (parent instanceof ScrollView) {
                parentScrollView = (ScrollView) parent;
            }
        }


        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = widthSize;
        int height = getMaxHeight();

        Log.i(TAG, "onMeasure --> widthMode,heightMode : " + widthMode + "," + heightMode + ":::widthSize,heightSize : " + widthSize + "," + heightSize);


        setMeasuredDimension(width, height);

    }


    private int getMaxHeight() {

        int size = childViewList.size();

        int col = size % colCount;
        int row = size / colCount;
        //当有余数，则加1行
        row = row + (col > 0 ? 1 : 0);

        int height = yPadding + (itemHeight + yPadding) * row;
        return height;
    }


    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (parentScrollView != null) {
            //父View 在屏幕上的坐标
            parentScrollView.getGlobalVisibleRect(parentRect);

        }
        Log.i(TAG, "onLayout 父View parentRect ：" + parentRect);

        layoutChildren(l, r);
        //指示View隐藏在第0个位置
        layoutViewToIndex(mIndicatorView, 0);

    }

    void layoutChildren(int l, int r) {
        int width = (r-l);

        xPadding = (width - (itemWidth * colCount)) / (colCount + 1);
        for (int i = 0; i < getChildViewCount(); i++) {
            if (i != draggedIndex) {
                layoutViewToIndex(getChildViewAt(i), i);
            }
        }
    }


    int getChildViewCount() {
        return childViewList.size();
    }

    public View getChildViewAt(int index) {
        return childViewList.get(index);
    }

    void removeAllChildView() {
        super.removeAllViews();
        childViewList.clear();
    }


    void layoutViewToIndex(View view, int index) {
        Point xy = getCoorFromIndex(index);
        view.layout(xy.x, xy.y, xy.x + itemWidth, xy.y + itemHeight);
    }


    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        //将正在移动的item放在最后一个绘制 防止出现正在移动的item被遮住的问题
        if (draggedIndex == -1) {
            return i;
        } else if (i == childCount - 1) {
            return draggedIndex;
        } else if (i >= draggedIndex) {
            return i + 1;
        }
        return i;
    }

    /**
     * get index from coordinate
     *
     * @param x
     * @param y
     * @return
     */
    private int getIndexFromCoor(int x, int y) {
        int col = getColFromCoor(x);
        int row = getRowFromCoor(y);
        if (col == -1 || row == -1) {
            return -1;
        }
        int index = row * colCount + col;
        if (index >= getChildViewCount()) {
            return -1;
        }
        return index;
    }

    private int getColFromCoor(int coor) {
        coor -= xPadding;
        for (int i = 0; coor > 0; i++) {
            if (coor < itemWidth) {
                return i;
            }
            coor -= (itemWidth + xPadding);
        }
        return -1;
    }

    private int getRowFromCoor(int coor) {
        coor -= yPadding;
        for (int i = 0; coor > 0; i++) {
            if (coor < itemHeight)
                return i;
            coor -= (itemHeight + yPadding);
        }
        return -1;
    }

    /**
     * 判断当前移动到的位置 当当前位置在另一个item区域时交换
     *
     * @param x
     * @param y
     * @return
     */
    private int getTargetFromCoor(int x, int y) {
        if (getRowFromCoor(y) == -1) {
            //touch is between rows
            return -1;
        }
        int target = getIndexFromCoor(x, y);
        //将item移动到最后的item之后
        if (target == getChildViewCount()) {
            target = getChildViewCount() - 1;
        }
        return target;
    }

    private Point getCoorFromIndex(int index) {
        int col = index % colCount;
        int row = index / colCount;
        return new Point(xPadding + (itemWidth + xPadding) * col,
                yPadding + (itemHeight + yPadding) * row);
    }

    @Override
    public void onClick(View view) {
        if (isMoveOverSlop()) {
            return;
        }

        if (onItemClickListener != null && getIndex() != -1) {
            onItemClickListener.onItemClick(null, getChildViewAt(getIndex()), getIndex(), getIndex() / colCount);
        }
    }

    //一个 item 子View被长按
    @Override
    public boolean onLongClick(View view) {
        Log.i(TAG, "onLongClick -- > " + view);
        if (isMoveOverSlop()) {
            //滑动距离大于阀值,认为是滑动不是长按
            //并且要返回true,代表此touch事件被消费了，不再传递
            return true;
        }
        //请求不允许父类拦截此touch事件
        requestDisallowInterceptTouchEvent(true);
        //修改为可移动状态
        changeItemViewToMoveable();

        int index = getIndex();
        if (index != -1) {
            //如果长按的位置在
            draggedIndex = index;
            animateActionDown();
            return true;
        }
        return false;
    }


    void changeItemViewToMoveable(){
        for (int i=0,size=childViewList.size();i<size;i++){
            ItemView view = (ItemView) childViewList.get(i);
            view.setStateMoveable();
        }
    }
    void changeItemViewToNormal(){
        for (int i=0,size=childViewList.size();i<size;i++){
            ItemView view = (ItemView) childViewList.get(i);
            view.setStateNormal();
        }
    }



    /**
     * 移动距离是否超过slop阀值
     */
    boolean isMoveOverSlop() {
        int temp_A = Math.abs(lastX - touchDownX);
        int temp_B = Math.abs(lastY - touchDownY);

        double deltaPath = java.lang.Math.sqrt(temp_A * temp_A + temp_B * temp_B);
        if (deltaPath > touchSlop) {
            //滑动距离大于阀值,认为是滑动
            return true;
        } else {
            return false;
        }
    }


    public boolean onTouch(View view, MotionEvent event) {
//        Log.i(TAG, "onTouch -- > MotionEvent : " + event);

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                //给touchDown赋值
                touchDownX = lastX;
                touchDownY = lastY;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) event.getX() - lastX;
                int deltaY = (int) event.getY() - lastY;
//                Log.i(TAG, "onTouch -- > ACTION_MOVE : " + event.getY() + ",event.getRawY()-->"+event.getRawY());


                if (draggedIndex != -1) {
                    int x = (int) event.getX(), y = (int) event.getY();
                    View draggedView = getChildViewAt(draggedIndex);
                    int itemLeft = draggedView.getLeft(), itemTop = draggedView.getTop();
                    draggedView.layout(itemLeft + deltaX, itemTop + deltaY, itemLeft + deltaX + itemWidth, itemTop + deltaY + itemHeight);
                    //得到当前点击位置所在的item的index
                    int targetIndex = getTargetFromCoor(x, y);
                    if (lastTargetIndex != targetIndex && targetIndex != -1) {
                        animateGap(targetIndex);
                        lastTargetIndex = targetIndex;
                    }

                    //移动指示View
                    moveIndicatorViewToIndex(targetIndex);
//                    Log.i(TAG, "deltaY : " + deltaY);
                    if (parentScrollView != null) {
//                        int rawX = Math.round(event.getRawX());
                        int rawY = Math.round(event.getRawY());
                        //滚动的慢一点
                        float ratio = 0.8f;

                        //父View向上滚动
                        if (((parentRect.bottom - rawY) < TOUCH_SPACE) && deltaY > 0) {
                            //滚动到底部了 并且 还是向下滚动
                            //滚动ScrollView
                            parentScrollView.scrollBy(0, Math.round(deltaY * ratio));

                        } else if ((rawY-parentRect.top) < TOUCH_SPACE && deltaY < 0) {
                            //滚动到顶部 并且还是向上滚动
                            parentScrollView.scrollBy(0, Math.round(deltaY * ratio));
                        }
                    }

                }

                lastX = (int) event.getX();
                lastY = (int) event.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (draggedIndex != -1) {
                    //如果存在item交换 则重新排列子view
                    if (lastTargetIndex != -1) {
                        reorderChildren();
                    }
                    animateActionUp();
                    lastTargetIndex = -1;
                    draggedIndex = -1;
                }
                //允许父scrollView拦截自己
                requestDisallowInterceptTouchEvent(false);
                changeItemViewToNormal();
                break;
        }
        //如果存在拖动item 则消费掉该事件
        if (draggedIndex != -1) {
//            Log.e(TAG, "onTouch -- > return : " + true);
            return true;
        } else {
//            Log.i(TAG, "onTouch -- > return : " + false);
            return false;
        }
    }

    /**
     * actionDown动画
     */
    private void animateActionDown() {
        View v = getChildViewAt(draggedIndex);
        AnimationSet animSet = new AnimationSet(true);
        AlphaAnimation alpha = new AlphaAnimation(1, .5f);
        alpha.setDuration(ANIM_DURATION);
        animSet.addAnimation(alpha);
        animSet.setFillEnabled(true);
        animSet.setFillAfter(true);
        v.clearAnimation();
        v.startAnimation(animSet);
    }

    /**
     * actionUp动画
     */
    private void animateActionUp() {
        View v = getChildViewAt(draggedIndex);
        AlphaAnimation alpha = new AlphaAnimation(.5f, 1);
        alpha.setDuration(ANIM_DURATION);
        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(alpha);
        animSet.setFillEnabled(true);
        animSet.setFillAfter(true);
        v.clearAnimation();
        v.startAnimation(animSet);
    }

    /**
     * 拖动某个item时其他item的移动动画
     * animate the other item when the dragged item moving
     *
     * @param targetIndex
     */
    private void animateGap(int targetIndex) {
        for (int i = 0; i < getChildViewCount(); i++) {
            View v = getChildViewAt(i);
            if (i == draggedIndex)
                continue;
            int newPos = i;
            if (draggedIndex < targetIndex && i >= draggedIndex + 1 && i <= targetIndex)
                newPos--;
            else if (targetIndex < draggedIndex && i >= targetIndex && i < draggedIndex)
                newPos++;

            //animate
            int oldPos = i;
            if (newPositions.get(i) != -1)
                oldPos = newPositions.get(i);
            if (oldPos == newPos)
                continue;

            Point oldXY = getCoorFromIndex(oldPos);
            Point newXY = getCoorFromIndex(newPos);
            Point oldOffset = new Point(oldXY.x - v.getLeft(), oldXY.y - v.getTop());
            Point newOffset = new Point(newXY.x - v.getLeft(), newXY.y - v.getTop());

            TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x,
                    Animation.ABSOLUTE, newOffset.x,
                    Animation.ABSOLUTE, oldOffset.y,
                    Animation.ABSOLUTE, newOffset.y);
            translate.setDuration(ANIM_DURATION);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);

            newPositions.set(i, newPos);
        }
    }

    private void reorderChildren() {
        //FIGURE OUT HOW TO REORDER CHILDREN WITHOUT REMOVING THEM ALL AND RECONSTRUCTING THE LIST!!!
        if (onRearrangeListener != null) {
            onRearrangeListener.onRearrange(draggedIndex, lastTargetIndex);
        }
        ArrayList<View> children = new ArrayList<View>();
        for (int i = 0; i < getChildViewCount(); i++) {
            getChildViewAt(i).clearAnimation();
            children.add(getChildViewAt(i));
        }
        removeAllChildView();
        while (draggedIndex != lastTargetIndex) {
            if (lastTargetIndex == children.size()) {
                // dragged and dropped to the right of the last element
                children.add(children.remove(draggedIndex));
                draggedIndex = lastTargetIndex;
            } else if (draggedIndex < lastTargetIndex) {
                // shift to the right
                Collections.swap(children, draggedIndex, draggedIndex + 1);
                draggedIndex++;
            } else if (draggedIndex > lastTargetIndex) {
                // shift to the left
                Collections.swap(children, draggedIndex, draggedIndex - 1);
                draggedIndex--;
            }
        }


        //添加indicatorView
        addIndicatorView(mIndicatorView);

        for (int i = 0; i < children.size(); i++) {
            newPositions.set(i, -1);
            addChildView(children.get(i));
        }
//		onLayout(true, getLeft(), getTop(), getRight(), getBottom());
        layoutChildren(getLeft(), getRight());
        //指示View隐藏在第0个位置
        layoutViewToIndex(mIndicatorView, 0);
    }

    /**
     * get the index of dragging item
     *
     * @return
     */
    private int getIndex() {
        return getIndexFromCoor(lastX, lastY);
    }

    public void setOnRearrangeListener(OnRearrangeListener onRearrangeListener) {
        this.onRearrangeListener = onRearrangeListener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }



    public interface OnRearrangeListener {

        void onRearrange(int oldIndex, int newIndex);
    }


    public static int dip2px(Context context, float dipValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    /**
     * DraggableGridView的ItemView
     */
    public static class ItemView extends FrameLayout{

        //正常状态
        public static final int STATE_NORMAL = 0;
        //可移动状态
        public static final int STATE_MOVEABLE = 1;
        //当前item状态
        private int mState = STATE_NORMAL;

        private final float ROUND;
        //虚线的间隔
        private int LINE_SPACE;
        private int LINE_WIDTH;

        //虚线的RectF
        private RectF mRectFLine = new RectF();
        private RectF mRectF = new RectF();
        private Paint paintRound;
        private Paint paintLine;

        private TextView tv_text_view;

        public ItemView(Context context) {
            this(context, null);
        }

        public ItemView(Context context, AttributeSet attrs) {
            super(context, attrs);
            //因为没有设置背景色，不调用此方法，不会执行onDraw
            setWillNotDraw(false);
            paintRound = createRoundRectPaint();
            ROUND = dip2px(context,2.5f);
            LINE_SPACE = dip2px(context,4);
            LINE_WIDTH = 1;
            paintLine = createLinePaint();
            init(context);
        }

        void init(Context context){

            tv_text_view = new TextView(context);
            tv_text_view.setGravity(Gravity.CENTER);
            tv_text_view.setTextColor(Color.parseColor("#747474"));
            tv_text_view.setTextSize(17);
            addView(tv_text_view);

        }


        Paint createRoundRectPaint(){
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#f2f2f2"));
            return paint;
        }

        Paint createLinePaint(){
            Paint p = new Paint();
            p.setFlags(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
//            p.setColor(Color.parseColor("#d2d2d2"));
            p.setColor(Color.RED);
            p.setStrokeWidth(LINE_WIDTH);
            PathEffect effects = new DashPathEffect(new float[] { LINE_SPACE,LINE_SPACE}, 1);
            p.setPathEffect(effects);
            return p;
        }

        public void setStateMoveable(){
            if (mState != STATE_MOVEABLE){
                mState = STATE_MOVEABLE;
                //重绘
                invalidate();
            }
        }
        public void setStateNormal(){
            if (mState != STATE_NORMAL){
                mState = STATE_NORMAL;
                //重绘
                invalidate();
            }
        }


        //设置文字
        public void setText(String text){
            tv_text_view.setText(text);
        }


        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
//            mRectF.set(left, top, right, bottom);
            Log.i(TAG, "left,top,right,bottom:" + left + "," + top + "," + right + "," + bottom);

        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            mRectF.set(0,0,w,h);
            int left = LINE_WIDTH;
            int top = LINE_WIDTH;
            int right = w - LINE_WIDTH;
            int bottom = h - LINE_WIDTH;
            //把虚线的宽度考虑进去
            mRectFLine.set(left,top,right,bottom);
            resetTextParams(w, h);
        }


        void resetTextParams(int w, int h){
            FrameLayout.LayoutParams params = (LayoutParams) tv_text_view.getLayoutParams();
            params.width = w;
            params.height = h;
            tv_text_view.setLayoutParams(params);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawRoundRect(mRectF, ROUND, ROUND, paintRound);
            if (mState == STATE_MOVEABLE){
                //可移动状态，画虚线
                drawRoundLine(canvas);
            }


        }


        protected void drawRoundLine(Canvas canvas){

            //画一个圆虚线
//            canvas.drawRoundRect(mRectF, ROUND,ROUND,p);
            canvas.drawRoundRect(mRectFLine,ROUND,ROUND,paintLine);
        }



    }
    /**
     * DraggableGridView的指示View
     */
    public static class IndicatorView extends ItemView{


        public IndicatorView(Context context) {
            this(context, null);
        }

        public IndicatorView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }


        @Override
        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
            //只画一个虚线
            drawRoundLine(canvas);
        }
    }


}
