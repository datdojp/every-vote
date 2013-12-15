package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.R;
import jp.co.mobilus.mobilib.observer.PcNotificationCenter;
import jp.co.mobilus.mobilib.observer.PcObserver;
import jp.co.mobilus.mobilib.util.PcInternal;
import jp.co.mobilus.mobilib.util.PcUtils;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public abstract class PcBaseFragmentActivity extends FragmentActivity implements PcObserver {

    // current status
    private int mOrientation;
    
    // wrapper views
    private View mContentView;
    private ViewGroup mInappPopupContainer;
    private PcDecorView mDecorView;

    // for background/foreground detecting
    private static long sLastOnPause = 0;
    private static Runnable sBackgroundStatusCheckTask = new Runnable() {
        @Override
        public void run() {
            PcNotificationCenter.postNotification(this, PcNotificationCenter.Name.Common.GO_TO_BACKGROUND);
        }
    };
    private static boolean sIsBackground;
    private static final long DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY = 2000;
    protected long mMaxAllowedTrasitionBetweenActivity = DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = PcInternal.getInstance().getCurrentContext();
        if (context == null || !(context instanceof Activity)) {
            PcInternal.getInstance().setCurrentContext(this);
        }
        mOrientation = getResources().getConfiguration().orientation;

        PcNotificationCenter.addObserver(this, PcNotificationCenter.Name.Common.GO_TO_BACKGROUND);
        PcNotificationCenter.addObserver(this, PcNotificationCenter.Name.Common.GO_TO_FOREGROUND);
    }

    @Override
    protected void onResume() {
        super.onResume();

        PcInternal.getInstance().setCurrentContext(this);

        PcInternal.getMainThread().removeCallbacks(sBackgroundStatusCheckTask);
        long now = getNow();
        if (now - sLastOnPause > mMaxAllowedTrasitionBetweenActivity) {
            PcNotificationCenter.postNotification(this, PcNotificationCenter.Name.Common.GO_TO_FOREGROUND);
        }

        if (mInappPopupContainer == null) {
            mInappPopupContainer = (ViewGroup) findViewById(R.id.pc_inapp_popup_container);

            // do not allow touch event below pop-up background
            mInappPopupContainer.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        PcUtils.hideKeyboard();

        sLastOnPause = getNow();
        PcInternal.getMainThread().postDelayed(sBackgroundStatusCheckTask, mMaxAllowedTrasitionBetweenActivity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            waitForWindowOrientationReallyChanged(new Runnable() {
                @Override
                public void run() {
                    PcNotificationCenter.postNotification(this, PcNotificationCenter.Name.Common.ORIENTATION_CHANGED);
                }
            });
        }
    }

    private void waitForWindowOrientationReallyChanged(final Runnable callback) {
        if (PcUtils.isPortraitDisplay() != PcUtils.isPortraitWindow()) {
            PcInternal.getMainThread().postDelayed(new Runnable() {
                @Override
                public void run() {
                    waitForWindowOrientationReallyChanged(callback);
                }
            }, 10);
        } else {
            callback.run();
        }
    }

    @Override
    public void onNotify(Object sender, String name, Object... args) {
        if (PcNotificationCenter.Name.Common.GO_TO_FOREGROUND.equals(name)) {
            if (isTopActivity()) {
                sIsBackground = false;
            }
        } else if (PcNotificationCenter.Name.Common.GO_TO_BACKGROUND.equals(name)) {
            if (isTopActivity()) {
                sIsBackground = true;
            }
        }
    }

    private long getNow() {
        return System.currentTimeMillis();
    }

    public boolean isBackground() {
        return sIsBackground;
    }

    public boolean isTopActivity() {
        return PcInternal.getInstance().getCurrentContext() == this;
    }

    private View createDecorViewAndAddContent(int layoutResId, LayoutParams params) {
        View content = getLayoutInflater().inflate(layoutResId, null);
        return createDecorViewAndAddContent(content, params);
    }

    private View createDecorViewAndAddContent(View layout, LayoutParams params) {
        if (params == null) {
            params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }
        layout.setLayoutParams(params);
        PcDecorView decorView = (PcDecorView) getLayoutInflater().inflate(R.layout.pc_decor_view, null);
        decorView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        decorView.addView(layout);
        mContentView = layout;

        mDecorView = decorView;

        return decorView;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(createDecorViewAndAddContent(layoutResID, null));
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(createDecorViewAndAddContent(view, null));
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
        super.setContentView(createDecorViewAndAddContent(view, params));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PcNotificationCenter.removeAllObserver(this);
    }

    public void showInAppPopup(final View content) {
        PcInternal.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                mInappPopupContainer.removeAllViews();
                mInappPopupContainer.addView(content);
                mInappPopupContainer.setVisibility(View.VISIBLE);
                mInappPopupContainer.bringToFront();
            }
        });
    }

    public void hideInAppPopup() {
        PcInternal.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                mInappPopupContainer.removeAllViews();
                mInappPopupContainer.setVisibility(View.GONE);
            }
        });
    }

    public boolean isInappPopupShown() {
        return mInappPopupContainer.getVisibility() == View.VISIBLE;
    }

    public PcDecorView getDecorView() {
        return mDecorView;
    }
    
    public View getContentView() {
        return mContentView;
    }
}
