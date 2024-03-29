package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.event.MblCommonEvents;
import jp.co.mobilus.mobilib.event.MblEventCenter;
import jp.co.mobilus.mobilib.util.MblUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class MblDecorView extends FrameLayout {
    private static final int KB_SHOWN = 1;
    private static final int KB_HIDDEN = 2;

    private static int sKeyboardStatus = 0;

    private int mMaxDisplaySize;
    private int mMinDisplaySize;
    private MlDecorViewOnSizeChangedDelegate mSizeChangedDelegate;


    public MblDecorView(Context context) {
        super(context);
        init();
    }

    public MblDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MblDecorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        int[] displaySizes = MblUtils.getDisplaySizes();
        mMaxDisplaySize = Math.max(displaySizes[0], displaySizes[1]);
        mMinDisplaySize = Math.min(displaySizes[0], displaySizes[1]);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mSizeChangedDelegate != null) mSizeChangedDelegate.onSizeChanged(w, h, oldw, oldh);

        if (getContext() != MblUtils.getCurrentContext()) return;

        int maxVisibleSize = Math.max(w, h);
        int minVisibleSize = Math.min(w, h);

        int maxDiff = Math.max(Math.abs(mMaxDisplaySize - maxVisibleSize), Math.abs(mMinDisplaySize - minVisibleSize));
        int kbStt = maxDiff >= MblUtils.getMinKeyboardHeight() ? KB_SHOWN : KB_HIDDEN;
        if (sKeyboardStatus != kbStt) {
            MblEventCenter.postNotification(this, MblCommonEvents.KEYBOARD_SHOW_OR_HIDE, kbStt == KB_SHOWN);
            sKeyboardStatus = kbStt;
        }
    }

    public static interface MlDecorViewOnSizeChangedDelegate {
        public void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    public void setSizeChangedDelegate(MlDecorViewOnSizeChangedDelegate delegate) {
        mSizeChangedDelegate = delegate;
    }
    
    public static boolean isKeyboardOn() {
        return sKeyboardStatus == KB_SHOWN;
    }
}
