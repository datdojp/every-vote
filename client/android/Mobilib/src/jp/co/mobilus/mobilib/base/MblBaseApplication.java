package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.util.MblUtils;
import android.app.Application;

public class MblBaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MblUtils.init(this);
    }
}
