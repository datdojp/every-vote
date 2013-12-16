package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.observer.MlNotificationCenter;
import jp.co.mobilus.mobilib.observer.MlObserver;
import android.support.v4.app.Fragment;

public abstract class MlBaseFragment extends Fragment {
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this instanceof MlObserver) {
            MlNotificationCenter.removeAllObserver((MlObserver) this);
        }
    }
}
