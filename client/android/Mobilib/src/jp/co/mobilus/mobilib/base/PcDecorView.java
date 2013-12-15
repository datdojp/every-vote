package jp.co.mobilus.mobilib.base;

import jp.co.pokelabo.pokechat.observer.PcNotificationCenter;
import jp.co.pokelabo.pokechat.util.PcInternal;
import jp.co.pokelabo.pokechat.util.PcUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class PcDecorView extends FrameLayout {
    private static final int KB_SHOWN = 1;
    private static final int KB_HIDDEN = 2;

    private static int sKeyboardStatus = 0;

    private int mMaxDisplaySize;
    private int mMinDisplaySize;
    private PcDecorViewOnSizeChangedDelegate mSizeChangedDelegate;


    public PcDecorView(Context context) {
        super(context);
        init();
    }

    public PcDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PcDecorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        int[] displaySizes = PcUtils.getDisplaySizes();
        mMaxDisplaySize = Math.max(displaySizes[0], displaySizes[1]);
        mMinDisplaySize = Math.min(displaySizes[0], displaySizes[1]);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mSizeChangedDelegate != null) mSizeChangedDelegate.onSizeChanged(w, h, oldw, oldh);

        if (getContext() != PcInternal.getInstance().getCurrentContext()) return;

        int maxVisibleSize = Math.max(w, h);
        int minVisibleSize = Math.min(w, h);

        int maxDiff = Math.max(Math.abs(mMaxDisplaySize - maxVisibleSize), Math.abs(mMinDisplaySize - minVisibleSize));
        int kbStt = maxDiff >= PcUtils.getMinKeyboardHeight() ? KB_SHOWN : KB_HIDDEN;
        if (sKeyboardStatus != kbStt) {
            PcNotificationCenter.postNotification(false, PcNotificationCenter.Name.Common.KEYBOARD_SHOW_OR_HIDE, kbStt == KB_SHOWN);
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
