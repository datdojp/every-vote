package jp.co.mobilus.mobilib.util;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

public class MlInternal {

    private static Handler sMainThread = new Handler(Looper.getMainLooper());
    private static Map<String, Object> sCommonBundle = new ConcurrentHashMap<String, Object>();

    private static SharedPreferences sPrefs;
    private Context mCurrentContext;

    private static MlInternal mInstance;
    public static MlInternal getInstance() {
        return mInstance;
    }

    private MlInternal(Context context) {
        mCurrentContext = context;
    }

    public static void createInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MlInternal(context);
        } else {
            mInstance.mCurrentContext = context;
        }
    }

    public static Handler getMainThread() {
        return sMainThread;
    }

    public SharedPreferences getPrefs() {
        return getPrefs(getCurrentContext());
    }

    public static SharedPreferences getPrefs(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return sPrefs;
    }

    public Context getCurrentContext() {
        return mCurrentContext;
    }

    public void setCurrentContext(Context context) {
        mCurrentContext = context;
    }

    public Locale getLocale() {
        if (mCurrentContext != null) {
            return mCurrentContext.getResources().getConfiguration().locale;
        } else {
            return Locale.JAPAN;
        }
    }

    public int getAvatarWidth() {
        return -1;
    }

    public int getAvatarHeight() {
        return -1;
    }

    public int getRoomImageWidth() {
        return -1;
    }

    public int getRoomImageHeight() {
        return -1;
    }

    @SuppressLint("NewApi")
    public static void executeOnAsyncThread(final Runnable action) {
        Assert.assertNotNull(action);
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                MlAsyncTask task = new MlAsyncTask() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        action.run();
                        return null;
                    }
                };
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    task.execute();
                
            }
        });
    }

    public static void executeOnMainThread(Runnable action) {
        Assert.assertNotNull(action);
        Context context = MlInternal.getInstance().getCurrentContext();
        if (context instanceof Activity) {
            ((Activity)context).runOnUiThread(action);
        } else {
            if (MlUtils.isMainThread()) {
                action.run();
            } else {
                sMainThread.post(action);
            }
        }
    }

    public static void putToCommonBundle(String key, Object value) {
        sCommonBundle.put(key, value);
    }

    public static String putToCommonBundle(Object value) {
        String key = UUID.randomUUID().toString();
        sCommonBundle.put(key, value);
        return key;
    }

    public static Object getFromCommonBundle(String key) {
        return sCommonBundle.get(key);
    }

    public static Object removeFromCommonBundle(String key) {
        return sCommonBundle.remove(key);
    }
}
