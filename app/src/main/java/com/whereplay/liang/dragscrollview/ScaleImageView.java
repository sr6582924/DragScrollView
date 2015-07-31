package com.whereplay.liang.dragscrollview;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.ImageView;


/**
 * Created by liang on 2015/7/27.
 */
public class ScaleImageView extends ImageView {

    private float mScaleX = 0.0f;
    private float mScaleY = 0.0f;
    private float mCurrentScaleX = 1.0f;
    private float mCurrentScaleY = 1.0f;
    private Matrix matrix=new Matrix();

    private int mViewWidth;
    private int mViewHeight;
    private int scaleTop, scaleLeft;

    public ScaleImageView(Context context) {
        super(context);
        init();
    }

    public ScaleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        setScaleImage(0, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void init() {
        setWillNotDraw(false);
        scaleTop = (int)getResources().getDimension(R.dimen.scaleview_top);
        scaleLeft = (int)getResources().getDimension(R.dimen.scaleview_left);
    }


    public void setScaleImage(float mScaleX, float mScaleY) {

        float scaleX = mCurrentScaleX + 0.35f * mScaleX;
        float scaleY = mCurrentScaleY + 0.35f * mScaleY;
        matrix.setTranslate(-scaleLeft, -scaleTop);
        matrix.postScale(scaleX, scaleY, mViewWidth / 2 + scaleLeft, scaleTop);
        setImageMatrix(matrix);
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }
}
