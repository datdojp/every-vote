package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.observer.PcNotificationCenter;
import jp.co.mobilus.mobilib.observer.PcObserver;
import android.support.v4.app.Fragment;

public abstract class PcBaseFragment extends Fragment {
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this instanceof PcObserver) {
            PcNotificationCenter.removeAllObserver((PcObserver) this);
        }
    }
}
