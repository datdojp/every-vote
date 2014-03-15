package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.event.MblCommonEvents;
import jp.co.mobilus.mobilib.event.MblEventCenter;
import jp.co.mobilus.mobilib.event.MblEventListener;
import jp.co.mobilus.mobilib.util.MblUtils;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public abstract class MblBaseActivity extends FragmentActivity {

    // current status
    private int mOrientation;
    
    // wrapper views
    private View mContentView;
    private MblDecorView mDecorView;

    // for background/foreground detecting
    private static long sLastOnPause = 0;
    private static Runnable sBackgroundStatusCheckTask = new Runnable() {
        @Override
        public void run() {
            MblEventCenter.postNotification(this, MblCommonEvents.GO_TO_BACKGROUND);
        }
    };
    private static final long DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY = 2000;
    protected long mMaxAllowedTrasitionBetweenActivity = DEFAULT_MAX_ALLOWED_TRASITION_BETWEEN_ACTIVITY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = MblUtils.getCurrentContext();
        if (context == null || !(context instanceof Activity)) {
            MblUtils.setCurrentContext(this);
        }
        mOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    protected void onResume() {
        super.onResume();

        MblUtils.setCurrentContext(this);

        MblUtils.getMainThreadHandler().removeCallbacks(sBackgroundStatusCheckTask);
        long now = getNow();
        if (now - sLastOnPause > mMaxAllowedTrasitionBetweenActivity) {
            MblEventCenter.postNotification(this, MblCommonEvents.GO_TO_FOREGROUND);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        MblUtils.hideKeyboard();

        sLastOnPause = getNow();
        MblUtils.getMainThreadHandler().postDelayed(sBackgroundStatusCheckTask, mMaxAllowedTrasitionBetweenActivity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            waitForWindowOrientationReallyChanged(new Runnable() {
                @Override
                public void run() {
                    MblEventCenter.postNotification(this, MblCommonEvents.ORIENTATION_CHANGED);
                }
            });
        }
    }

    private void waitForWindowOrientationReallyChanged(final Runnable callback) {
        if (MblUtils.isPortraitDisplay() != MblUtils.isPortraitWindow()) {
            MblUtils.getMainThreadHandler().postDelayed(new Runnable() {
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
        return MblUtils.getCurrentContext() == this;
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
        MblDecorView decorView = new MblDecorView(this);
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
        if (this instanceof MblEventListener) {
            MblEventCenter.removeAllObserver((MblEventListener) this);
        }
    }

    public MblDecorView getDecorView() {
        return mDecorView;
    }
    
    public View getContentView() {
        return mContentView;
    }
}
