package jp.co.mobilus.mobilib.base;

import jp.co.mobilus.mobilib.event.MblEventCenter;
import jp.co.mobilus.mobilib.event.MblEventListener;
import android.support.v4.app.Fragment;

public abstract class MblBaseFragment extends Fragment {
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this instanceof MblEventListener) {
            MblEventCenter.removeAllObserver((MblEventListener) this);
        }
    }
}
