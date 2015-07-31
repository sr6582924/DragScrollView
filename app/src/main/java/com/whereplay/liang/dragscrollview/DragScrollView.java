package com.whereplay.liang.dragscrollview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

/**
 * Created by liang on 2015/7/30.
 */
public class DragScrollView extends LinearLayout{

    /**
     * 当前的状态
     */
    public enum State {

        /**
         * Initial state
         */
        NONE,

        /**
         * When the UI is in a state which means that user is not interacting
         * with the Pull-to-Refresh function.
         */
        RESET,

        /**
         * When the UI is being pulled by the user, but has not been pulled far
         * enough so that it refreshes when released.
         */
        PULL_TO_REFRESH,

        /**
         * When the UI is being pulled by the user, and <strong>has</strong>
         * been pulled far enough so that it will refresh when released.
         */
        RELEASE_TO_REFRESH,

        /**
         * When the UI is currently refreshing, caused by a pull gesture.
         */
        REFRESHING,

        /**
         * When the UI is currently refreshing, caused by a pull gesture.
         */
        @Deprecated
        LOADING,

        /**
         * No more data
         */
        NO_MORE_DATA,
    }

    /**回滚的时间*/
    private static final int SCROLL_DURATION = 150;
    /**阻尼系数*/
    private static final float OFFSET_RADIO = 2.8f;
    /**上一次移动的点 */
    private float mLastMotionY = -1;

    /**是否截断touch事件*/
    private boolean mInterceptEventEnable = true;
    /**表示是否消费了touch事件，如果是，则不调用父类的onTouchEvent方法*/
    private boolean mIsHandledTouchEvent = false;
    /**移动点的保护范围值*/
    private int mTouchSlop;

    /**平滑滚动的Runnable*/
    private SmoothScrollRunnable mSmoothScrollRunnable;

    private float mDensity;

    private ScaleImageView scaleView;

    private LinearLayout moveView;

    private State mPullDownState = State.NONE;

    /**可以下拉刷新的View*/
    private View mDropdownView;

    /**HeaderView的高度*/
    private int mHeaderHeight;

    public DragScrollView(Context context) {
        this(context, null);
    }

    public DragScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = getResources().getDisplayMetrics().density;
        mHeaderHeight = (int)(mDensity * 100.0f);
    }

    public LinearLayout getMoveView() {
        return moveView;
    }

    public void setMoveView(LinearLayout moveView) {
        this.moveView = moveView;
    }

    public ScaleImageView getScaleView() {
        return scaleView;
    }

    public void setScaleView(ScaleImageView scaleView) {
        this.scaleView = scaleView;
    }

    public View getDropdownView() {
        return mDropdownView;
    }

    public void setDropdownView(View mDropdownView) {
        this.mDropdownView = mDropdownView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        if (!isInterceptTouchEventEnabled()) {
            return false;
        }
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsHandledTouchEvent = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsHandledTouchEvent) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = event.getY();
                mIsHandledTouchEvent = false;
                break;

            case MotionEvent.ACTION_MOVE:
                final float deltaY = event.getY() - mLastMotionY;
                final float absDiff = Math.abs(deltaY);
                // 这里有三个条件：
                // 1，位移差大于mTouchSlop，这是为了防止快速拖动引发刷新
                // 2，isPullRefreshing()，如果当前正在下拉刷新的话，是允许向上滑动，并把刷新的HeaderView挤上去
                // 3，isPullLoading()，理由与第2条相同


                if (absDiff > mTouchSlop || isPullRefreshing())  {
                    mLastMotionY = event.getY();
                    // 第一个显示出来，Header已经显示或拉下
                    if (isReadyForPullDown()) {
                        // 1，Math.abs(getScrollY()) > 0：表示当前滑动的偏移量的绝对值大于0，表示当前HeaderView滑出来了或完全
                        // 不可见，存在这样一种case，当正在刷新时并且RefreshableView已经滑到顶部，向上滑动，那么我们期望的结果是
                        // 依然能向上滑动，直到HeaderView完全不可见
                        // 2，deltaY > 0.5f：表示下拉的值大于0.5f
                        mIsHandledTouchEvent = (Math.abs(getScrollYValue()) > 0 || deltaY > 0.5f);
                        // 如果截断事件，我们则仍然把这个事件交给刷新View去处理，典型的情况是让ListView/GridView将按下
                        // Child的Selector隐藏
                        if (mIsHandledTouchEvent && mDropdownView != null) {
                            mDropdownView.onTouchEvent(event);
                        }
                    }
                }
                break;

            default:
                break;
        }
        return mIsHandledTouchEvent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean handled = false;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = event.getY();
                mIsHandledTouchEvent = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float delta = event.getY() - mLastMotionY;
                mLastMotionY = event.getY();
                if (isReadyForPullDown()) {
                    handleDragEvent(delta / OFFSET_RADIO);
                    handled = true;
                } else {
                    mIsHandledTouchEvent = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isReadyForPullDown()) {
                    // 调用刷新
                    if ((mPullDownState == State.RELEASE_TO_REFRESH)) {
                        startRefreshing();
                        handled = true;
                    }
                    resetHeaderLayout();
                }
                break;
        }
        return handled;
    }

    /**
     * 得置header
     */
    protected void resetHeaderLayout() {
        final int scrollY = Math.abs(getScrollYValue());
        final boolean refreshing = isPullRefreshing();

        if (refreshing && scrollY <= mHeaderHeight) {
            smoothScrollTo(0);
            return;
        }

        if (refreshing) {
            smoothScrollTo(0);
        } else {
            smoothScrollTo(0);
        }
    }

    /**
     * 开始刷新，当下拉松开后被调用
     */
    protected void startRefreshing() {
        // 如果正在刷新
        if (isPullRefreshing()) {
            return;
        }

        mPullDownState = State.REFRESHING;
    }

    protected  boolean isReadyForPullDown() {
        return mDropdownView.getScrollY() == 0;
    }

    public void handleDragEvent(float delta) {
        int oldScrollY = getScrollYValue();
        //Logx.d("pullHeaderLayout delta:" + delta + "oldScrollY:" + oldScrollY + "(oldScrollY - delta):" + (oldScrollY - delta));
        if (delta < 0 && (oldScrollY - delta) >= 0) {
            setScrollTo(0, 0);
            return;
        }
        if (Math.abs(oldScrollY) < mHeaderHeight)
             setScrollBy(0, -(int)delta);
        if (0 != mHeaderHeight) {
            float scale = Math.abs(getScrollYValue()) / (float) mHeaderHeight;
            smoonScaleView(scale);
        }

        // 未处于刷新状态，更新箭头
        int scrollY = Math.abs(getScrollYValue());
        if (!isPullRefreshing()) {
            if (scrollY > (mHeaderHeight / 2)) {
                mPullDownState = State.RELEASE_TO_REFRESH;
            } else {
                mPullDownState = State.PULL_TO_REFRESH;
            }
        }
    }

    private void smoonScaleView(float scale) {
        scaleView.setScaleImage(scale, scale);
    }

    /**
     * 判断是否正在下拉刷新
     *
     * @return true正在刷新，否则false
     */
    protected boolean isPullRefreshing() {
        return (mPullDownState == State.REFRESHING);
    }

    /**
     * 设置滚动位置
     *
     * @param x 滚动到的x位置
     * @param y 滚动到的y位置
     */
    private void setScrollTo(int x, int y) {
        moveView.scrollTo(x, y);
    }

    /**
     * 设置滚动的偏移
     *
     * @param x 滚动x位置
     * @param y 滚动y位置
     */
    private void setScrollBy(int x, int y) {
        moveView.scrollBy(x, y);
    }

    /**
     * 得到当前Y的滚动值
     *
     * @return 滚动值
     */
    private int getScrollYValue() {
        return moveView.getScrollY();
    }

    /**
     * 平滑滚动
     *
     * @param newScrollValue 滚动的值
     */
    private void smoothScrollTo(int newScrollValue) {
        smoothScrollTo(newScrollValue, getSmoothScrollDuration(), 0);
    }


    /**
     * 得到平滑滚动的时间，派生类可以重写这个方法来控件滚动时间
     *
     * @return 返回值时间为毫秒
     */
    protected long getSmoothScrollDuration() {
        return SCROLL_DURATION;
    }

    /**
     * 平滑滚动
     *
     * @param newScrollValue 滚动的值
     * @param duration 滚动时候
     * @param delayMillis 延迟时间，0代表不延迟
     */
    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis) {
        if (null != mSmoothScrollRunnable) {
            mSmoothScrollRunnable.stop();
        }
        int oldScrollValue = this.getScrollYValue();
        boolean post = (oldScrollValue != newScrollValue);
        if (post) {
            mSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration);
        }

        if (post) {
            if (delayMillis > 0) {
                postDelayed(mSmoothScrollRunnable, delayMillis);
            } else {
                post(mSmoothScrollRunnable);
            }
        }
    }

    /**
     * 设置是否截断touch事件
     *
     * @param enabled true截断，false不截断
     */
    private void setInterceptTouchEventEnabled(boolean enabled) {
        mInterceptEventEnable = enabled;
    }

    /**
     * 标志是否截断touch事件
     *
     * @return true截断，false不截断
     */
    private boolean isInterceptTouchEventEnabled() {
        return mInterceptEventEnable;
    }

    /**
     * 实现了平滑滚动的Runnable
     *
     * @author Li Hong
     * @since 2013-8-22
     */
    final class SmoothScrollRunnable implements Runnable {

        /**动画效果*/
        private final Interpolator mInterpolator;
        /**结束Y*/
        private final int mScrollToY;
        /**开始Y*/
        private final int mScrollFromY;
        /**滑动时间*/
        private final long mDuration;
        /**是否继续运行*/
        private boolean mContinueRunning = true;
        /**开始时刻*/
        private long mStartTime = -1;
        /**当前Y*/
        private int mCurrentY = -1;

        /**
         * 构造方法
         *
         * @param fromY 开始Y
         * @param toY 结束Y
         * @param duration 动画时间
         */
        public SmoothScrollRunnable(int fromY, int toY, long duration) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mDuration = duration;
            mInterpolator = new DecelerateInterpolator();
        }

        @Override
        public void run() {

            /**
             * If the duration is 0, we scroll the view to target y directly.
             */
            if (mDuration <= 0) {

                setScrollTo(0, mScrollToY);
                return;
            }

            /**
             * Only set mStartTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {
                /**
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                final long oneSecond = 1000;    // SUPPRESS CHECKSTYLE
                long normalizedTime = (oneSecond * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, oneSecond), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY)
                        * mInterpolator.getInterpolation(normalizedTime / (float) oneSecond));
                mCurrentY = mScrollFromY - deltaY;
                setScrollTo(0, mCurrentY);
                float scale = Math.abs(getScrollYValue()) / (float) mHeaderHeight;
                smoonScaleView(scale);
            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && mScrollToY != mCurrentY) {
                DragScrollView.this.postDelayed(this, 16);// SUPPRESS CHECKSTYLE
            }

        }

        /**
         * 停止滑动
         */
        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }


}
