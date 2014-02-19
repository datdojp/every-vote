package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.observer.MlNotificationCenter;
import jp.co.mobilus.mobilib.observer.MlObserver;
import jp.co.mobilus.mobilib.util.MlUtils;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public abstract class MlBaseActivity extends FragmentActivity {

    // current status
    private int mOrientation;
    
    // wrapper views
    private View mContentView;
    private MlDecorView mDecorView;

    // for background/foreground detecting
    private static long sLastOnPause = 0;
    private static Runnable sBackgroundStatusCheckTask = new Runnable() {
        @Override
        public void run() {
            MlNotificationCenter.postNotification(this, MlNotificationCenter.Name.Common.GO_TO_BACKGROUND);
        }
    };
    private static final long DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY = 2000;
    protected long mMaxAllowedTrasitionBetweenActivity = DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = MlUtils.getCurrentContext();
        if (context == null || !(context instanceof Activity)) {
            MlUtils.setCurrentContext(this);
        }
        mOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    protected void onResume() {
        super.onResume();

        MlUtils.setCurrentContext(this);

        MlUtils.getMainThreadHandler().removeCallbacks(sBackgroundStatusCheckTask);
        long now = getNow();
        if (now - sLastOnPause > mMaxAllowedTrasitionBetweenActivity) {
            MlNotificationCenter.postNotification(this, MlNotificationCenter.Name.Common.GO_TO_FOREGROUND);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        MlUtils.hideKeyboard();

        sLastOnPause = getNow();
        MlUtils.getMainThreadHandler().postDelayed(sBackgroundStatusCheckTask, mMaxAllowedTrasitionBetweenActivity);
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
            MlUtils.getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    waitForWindowOrientationReallyChanged(callback);
                }
            }, 10);
        } else {
            callback.run();
        }
    }

    private long getNow() {
        return System.currentTimeMillis();
    }

    public boolean isTopActivity() {
        return MlUtils.getCurrentContext() == this;
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
        MlDecorView decorView = new MlDecorView(MlUtils.getCurrentContext());
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
        if (this instanceof MlObserver) {
            MlNotificationCenter.removeAllObserver((MlObserver) this);
        }
    }

    public MlDecorView getDecorView() {
        return mDecorView;
    }
    
    public View getContentView() {
        return mContentView;
    }
}
