package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.observer.MblNotificationCenter;
import jp.co.mobilus.mobilib.observer.MblObserver;
import android.support.v4.app.Fragment;

public abstract class MblBaseFragment extends Fragment {
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this instanceof MblObserver) {
            MblNotificationCenter.removeAllObserver((MblObserver) this);
        }
    }
}
