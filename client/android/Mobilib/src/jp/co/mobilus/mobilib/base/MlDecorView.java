package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.observer.MlNotificationCenter;
import jp.co.mobilus.mobilib.util.MlInternal;
import jp.co.mobilus.mobilib.util.MlUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class MlDecorView extends FrameLayout {
    private static final int KB_SHOWN = 1;
    private static final int KB_HIDDEN = 2;

    private static int sKeyboardStatus = 0;

    private int mMaxDisplaySize;
    private int mMinDisplaySize;
    private PcDecorViewOnSizeChangedDelegate mSizeChangedDelegate;


    public MlDecorView(Context context) {
        super(context);
        init();
    }

    public MlDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MlDecorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        int[] displaySizes = MlUtils.getDisplaySizes();
        mMaxDisplaySize = Math.max(displaySizes[0], displaySizes[1]);
        mMinDisplaySize = Math.min(displaySizes[0], displaySizes[1]);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mSizeChangedDelegate != null) mSizeChangedDelegate.onSizeChanged(w, h, oldw, oldh);

        if (getContext() != MlInternal.getInstance().getCurrentContext()) return;

        int maxVisibleSize = Math.max(w, h);
        int minVisibleSize = Math.min(w, h);

        int maxDiff = Math.max(Math.abs(mMaxDisplaySize - maxVisibleSize), Math.abs(mMinDisplaySize - minVisibleSize));
        int kbStt = maxDiff >= MlUtils.getMinKeyboardHeight() ? KB_SHOWN : KB_HIDDEN;
        if (sKeyboardStatus != kbStt) {
            MlNotificationCenter.postNotification(false, MlNotificationCenter.Name.Common.KEYBOARD_SHOW_OR_HIDE, kbStt == KB_SHOWN);
            sKeyboardStatus = kbStt;
        }
    }

    public static interface PcDecorViewOnSizeChangedDelegate {
        public void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    public void setSizeChangedDelegate(PcDecorViewOnSizeChangedDelegate delegate) {
        mSizeChangedDelegate = delegate;
    }
    
    public static boolean isKeyboardOn() {
        return sKeyboardStatus == KB_SHOWN;
    }
}
