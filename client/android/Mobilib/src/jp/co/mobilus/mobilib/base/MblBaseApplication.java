package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.util.MblUtils;
import android.app.Application;
import android.text.TextUtils;

public abstract class MblBaseApplication extends Application {

    private static final String PREF_VERSION_CODE = MblBaseApplication.class + "version_code";
    private static final String PREF_VERSION_NAME = MblBaseApplication.class + "version_name";

    @Override
    public void onCreate() {
        super.onCreate();
        MblUtils.init(this);

        // check version-code changed
        int versionCode = MblUtils.getAppPackageInfo().versionCode;
        int prefVersionCode = MblUtils.getPrefs().getInt(PREF_VERSION_CODE, -1);
        if (prefVersionCode < 0 || prefVersionCode != versionCode) {
            onVersionCodeChanged(prefVersionCode, versionCode);
            MblUtils.getPrefs()
            .edit()
            .putInt(PREF_VERSION_CODE, versionCode)
            .commit();
        }

        // check version-name changed
        String versionName = MblUtils.getAppPackageInfo().versionName;
        String prefVersionName = MblUtils.getPrefs().getString(PREF_VERSION_NAME, null);
        if (prefVersionName == null || !TextUtils.equals(versionName, prefVersionName)) {
            onVersionNameChanged(prefVersionName, versionName);
            MblUtils.getPrefs()
            .edit()
            .putString(PREF_VERSION_NAME, versionName)
            .commit();
        }
    }

    public abstract void onVersionCodeChanged(int oldVersionCode, int newVersionCode);
    public abstract void onVersionNameChanged(String oldVersionName, String newVersionName);
}
