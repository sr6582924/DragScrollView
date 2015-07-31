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
     * ��ǰ��״̬
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

    /**�ع���ʱ��*/
    private static final int SCROLL_DURATION = 150;
    /**����ϵ��*/
    private static final float OFFSET_RADIO = 2.8f;
    /**��һ���ƶ��ĵ� */
    private float mLastMotionY = -1;

    /**�Ƿ�ض�touch�¼�*/
    private boolean mInterceptEventEnable = true;
    /**��ʾ�Ƿ�������touch�¼�������ǣ��򲻵��ø����onTouchEvent����*/
    private boolean mIsHandledTouchEvent = false;
    /**�ƶ���ı�����Χֵ*/
    private int mTouchSlop;

    /**ƽ��������Runnable*/
    private SmoothScrollRunnable mSmoothScrollRunnable;

    private float mDensity;

    private ScaleImageView scaleView;

    private LinearLayout moveView;

    private State mPullDownState = State.NONE;

    /**��������ˢ�µ�View*/
    private View mDropdownView;

    /**HeaderView�ĸ߶�*/
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
                // ����������������
                // 1��λ�Ʋ����mTouchSlop������Ϊ�˷�ֹ�����϶�����ˢ��
                // 2��isPullRefreshing()�������ǰ��������ˢ�µĻ������������ϻ���������ˢ�µ�HeaderView����ȥ
                // 3��isPullLoading()���������2����ͬ


                if (absDiff > mTouchSlop || isPullRefreshing())  {
                    mLastMotionY = event.getY();
                    // ��һ����ʾ������Header�Ѿ���ʾ������
                    if (isReadyForPullDown()) {
                        // 1��Math.abs(getScrollY()) > 0����ʾ��ǰ������ƫ�����ľ���ֵ����0����ʾ��ǰHeaderView�������˻���ȫ
                        // ���ɼ�����������һ��case��������ˢ��ʱ����RefreshableView�Ѿ��������������ϻ�������ô���������Ľ����
                        // ��Ȼ�����ϻ�����ֱ��HeaderView��ȫ���ɼ�
                        // 2��deltaY > 0.5f����ʾ������ֵ����0.5f
                        mIsHandledTouchEvent = (Math.abs(getScrollYValue()) > 0 || deltaY > 0.5f);
                        // ����ض��¼�����������Ȼ������¼�����ˢ��Viewȥ�������͵��������ListView/GridView������
                        // Child��Selector����
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
                    // ����ˢ��
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
     * ����header
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
     * ��ʼˢ�£��������ɿ��󱻵���
     */
    protected void startRefreshing() {
        // �������ˢ��
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

        // δ����ˢ��״̬�����¼�ͷ
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
     * �ж��Ƿ���������ˢ��
     *
     * @return true����ˢ�£�����false
     */
    protected boolean isPullRefreshing() {
        return (mPullDownState == State.REFRESHING);
    }

    /**
     * ���ù���λ��
     *
     * @param x ��������xλ��
     * @param y ��������yλ��
     */
    private void setScrollTo(int x, int y) {
        moveView.scrollTo(x, y);
    }

    /**
     * ���ù�����ƫ��
     *
     * @param x ����xλ��
     * @param y ����yλ��
     */
    private void setScrollBy(int x, int y) {
        moveView.scrollBy(x, y);
    }

    /**
     * �õ���ǰY�Ĺ���ֵ
     *
     * @return ����ֵ
     */
    private int getScrollYValue() {
        return moveView.getScrollY();
    }

    /**
     * ƽ������
     *
     * @param newScrollValue ������ֵ
     */
    private void smoothScrollTo(int newScrollValue) {
        smoothScrollTo(newScrollValue, getSmoothScrollDuration(), 0);
    }


    /**
     * �õ�ƽ��������ʱ�䣬�����������д����������ؼ�����ʱ��
     *
     * @return ����ֵʱ��Ϊ����
     */
    protected long getSmoothScrollDuration() {
        return SCROLL_DURATION;
    }

    /**
     * ƽ������
     *
     * @param newScrollValue ������ֵ
     * @param duration ����ʱ��
     * @param delayMillis �ӳ�ʱ�䣬0�����ӳ�
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
     * �����Ƿ�ض�touch�¼�
     *
     * @param enabled true�ضϣ�false���ض�
     */
    private void setInterceptTouchEventEnabled(boolean enabled) {
        mInterceptEventEnable = enabled;
    }

    /**
     * ��־�Ƿ�ض�touch�¼�
     *
     * @return true�ضϣ�false���ض�
     */
    private boolean isInterceptTouchEventEnabled() {
        return mInterceptEventEnable;
    }

    /**
     * ʵ����ƽ��������Runnable
     *
     * @author Li Hong
     * @since 2013-8-22
     */
    final class SmoothScrollRunnable implements Runnable {

        /**����Ч��*/
        private final Interpolator mInterpolator;
        /**����Y*/
        private final int mScrollToY;
        /**��ʼY*/
        private final int mScrollFromY;
        /**����ʱ��*/
        private final long mDuration;
        /**�Ƿ��������*/
        private boolean mContinueRunning = true;
        /**��ʼʱ��*/
        private long mStartTime = -1;
        /**��ǰY*/
        private int mCurrentY = -1;

        /**
         * ���췽��
         *
         * @param fromY ��ʼY
         * @param toY ����Y
         * @param duration ����ʱ��
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
         * ֹͣ����
         */
        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }


}
