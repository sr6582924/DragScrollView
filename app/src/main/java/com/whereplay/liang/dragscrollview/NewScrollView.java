package com.whereplay.liang.dragscrollview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by liang on 2015/7/30.
 */
public class NewScrollView extends ScrollView{

    private OnScrollListener onScrollListener;

    public interface OnScrollListener {
        public void onScroll(int x, int y);
    }

    public NewScrollView(Context context) {
        super(context);
    }

    public NewScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OnScrollListener getOnScrollListener() {
        return onScrollListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        Logx.d("scrollBy x:" + l + " y:" + t);
        if (onScrollListener != null) {
            onScrollListener.onScroll(l, t);
        }
    }
}
