package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.util.MlUtils;
import android.app.Application;

public class MlBaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MlUtils.init(this);
    }
}
