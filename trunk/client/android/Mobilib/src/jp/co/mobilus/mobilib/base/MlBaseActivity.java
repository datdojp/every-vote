package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.R;
import jp.co.mobilus.mobilib.observer.MlNotificationCenter;
import jp.co.mobilus.mobilib.observer.MlObserver;
import jp.co.mobilus.mobilib.util.MlInternal;
import jp.co.mobilus.mobilib.util.MlUtils;
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

public abstract class MlBaseActivity extends FragmentActivity implements MlObserver {

    // current status
    private int mOrientation;
    
    // wrapper views
    private View mContentView;
    private ViewGroup mInappPopupContainer;
    private MlDecorView mDecorView;

    // for background/foreground detecting
    private static long sLastOnPause = 0;
    private static Runnable sBackgroundStatusCheckTask = new Runnable() {
        @Override
        public void run() {
            MlNotificationCenter.postNotification(this, MlNotificationCenter.Name.Common.GO_TO_BACKGROUND);
        }
    };
    private static boolean sIsBackground;
    private static final long DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY = 2000;
    protected long mMaxAllowedTrasitionBetweenActivity = DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = MlInternal.getInstance().getCurrentContext();
        if (context == null || !(context instanceof Activity)) {
            MlInternal.getInstance().setCurrentContext(this);
        }
        mOrientation = getResources().getConfiguration().orientation;

        MlNotificationCenter.addObserver(this, MlNotificationCenter.Name.Common.GO_TO_BACKGROUND);
        MlNotificationCenter.addObserver(this, MlNotificationCenter.Name.Common.GO_TO_FOREGROUND);
    }

    @Override
    protected void onResume() {
        super.onResume();

        MlInternal.getInstance().setCurrentContext(this);

        MlInternal.getMainThread().removeCallbacks(sBackgroundStatusCheckTask);
        long now = getNow();
        if (now - sLastOnPause > mMaxAllowedTrasitionBetweenActivity) {
            MlNotificationCenter.postNotification(this, MlNotificationCenter.Name.Common.GO_TO_FOREGROUND);
        }

        if (mInappPopupContainer == null) {
            mInappPopupContainer = (ViewGroup) findViewById(R.id.ml_inapp_popup_container);

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

        MlUtils.hideKeyboard();

        sLastOnPause = getNow();
        MlInternal.getMainThread().postDelayed(sBackgroundStatusCheckTask, mMaxAllowedTrasitionBetweenActivity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            waitForWindowOrientationReallyChanged(new Runnable() {
                @Override
                public void run() {
                    MlNotificationCenter.postNotification(this, MlNotificationCenter.Name.Common.ORIENTATION_CHANGED);
                }
            });
        }
    }

    private void waitForWindowOrientationReallyChanged(final Runnable callback) {
        if (MlUtils.isPortraitDisplay() != MlUtils.isPortraitWindow()) {
            MlInternal.getMainThread().postDelayed(new Runnable() {
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
        if (MlNotificationCenter.Name.Common.GO_TO_FOREGROUND.equals(name)) {
            if (isTopActivity()) {
                sIsBackground = false;
            }
        } else if (MlNotificationCenter.Name.Common.GO_TO_BACKGROUND.equals(name)) {
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
        return MlInternal.getInstance().getCurrentContext() == this;
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
        MlDecorView decorView = (MlDecorView) getLayoutInflater().inflate(R.layout.ml_decor_view, null);
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
        MlNotificationCenter.removeAllObserver(this);
    }

    public void showInAppPopup(final View content) {
        MlInternal.executeOnMainThread(new Runnable() {
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
        MlInternal.executeOnMainThread(new Runnable() {
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

    public MlDecorView getDecorView() {
        return mDecorView;
    }
    
    public View getContentView() {
        return mContentView;
    }
}
