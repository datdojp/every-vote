package jp.co.mobilus.mobilib.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class PcListViewWithScrollableItems extends ListView {
    private Delegate mDelegate;

    public PcListViewWithScrollableItems(Context context) {
        super(context);
    }

    public PcListViewWithScrollableItems(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PcListViewWithScrollableItems(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mDelegate != null && mDelegate.shouldNOTInterceptTouchEvent(event)) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    };
    
    
    
    public static interface Delegate {
        public boolean shouldNOTInterceptTouchEvent(MotionEvent event);
    }

    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }
}