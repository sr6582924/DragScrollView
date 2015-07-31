package com.whereplay.liang.dragscrollview;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;


public class MainActivity extends Activity {

    private ScaleImageView photo_siv;

    private LinearLayout contentPanel_ll;

    private LinearLayout indicator_top;
    private LinearLayout indicator_bottom;
    private NewScrollView scrollView;

    private int indicatorTopY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);
        DragScrollView refScrollView = (DragScrollView) findViewById(R.id.pulltoRefreshScroll);
        scrollView = (NewScrollView) findViewById(R.id.myscroll_sv);
        refScrollView.setDropdownView(scrollView);
        photo_siv = (ScaleImageView) findViewById(R.id.photo_siv);
        contentPanel_ll = (LinearLayout) findViewById(R.id.contentPanel_ll);
        indicator_top = (LinearLayout) findViewById(R.id.indicator_top);
        indicator_bottom = (LinearLayout) findViewById(R.id.indicator_bottom);
        refScrollView.setScaleView(photo_siv);
        refScrollView.setMoveView(contentPanel_ll);
        scrollView.setOnScrollListener(new NewScrollView.OnScrollListener() {
            @Override
            public void onScroll(int x, int y) {
                if (scrollView.getScrollY() >= indicatorTopY) {
                    if (indicator_top.getVisibility() == View.INVISIBLE)
                        indicator_top.setVisibility(View.VISIBLE);
                } else {
                    if (indicator_top.getVisibility() == View.VISIBLE)
                        indicator_top.setVisibility(View.INVISIBLE);
                }
            }
        });



    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        int[] scrollLocation = getViewLocation(scrollView);
        int[] indicatorLoaction = getViewLocation(indicator_bottom);
        indicatorTopY = indicatorLoaction[1] - scrollLocation[1];
    }

    public int[] getViewLocation(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return location;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
