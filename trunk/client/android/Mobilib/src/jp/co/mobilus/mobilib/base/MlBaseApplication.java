package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.util.MlInternal;
import android.app.Application;

public class MlBaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MlInternal.createInstance(this);
    }
}
