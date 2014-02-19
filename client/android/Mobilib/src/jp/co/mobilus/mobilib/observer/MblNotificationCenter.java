package jp.co.mobilus.mobilib.observer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.mobilus.mobilib.observer.MblWeakArrayList.MlWeakArrayListCallback;
import android.os.Handler;

public class MblNotificationCenter {
    public static class Name {
        public static class Common {
            public static final String ORIENTATION_CHANGED              = Common.class + "orientation_changed";
            public static final String NETWORK_STATUS_CHANGED           = Common.class + "network_status_changed";
            public static final String KEYBOARD_SHOW_OR_HIDE            = Common.class + "keyboard_show_or_hide";
            public static final String GO_TO_BACKGROUND                 = Common.class + "go_to_background";
            public static final String GO_TO_FOREGROUND                 = Common.class + "go_to_foreground";
        }
    }

    private static final Map<String, MblWeakArrayList<MblObserver>> mObserverMap = new ConcurrentHashMap<String, MblWeakArrayList<MblObserver>>();
    private static final Handler sMainThread = new Handler();

    private MblNotificationCenter() {}

    public static void addObserver(MblObserver observer, String name) {
        MblWeakArrayList<MblObserver> observers = null;

        if(mObserverMap.containsKey(name)) {
            observers = mObserverMap.get(name);
        } else {
            observers = new MblWeakArrayList<MblObserver>();
            mObserverMap.put(name, observers);
        }
        if(observers.contains(observer)) return;

        observers.add(observer);
    }

    public static void removeObserver(MblObserver observer, String name) {
        if(!mObserverMap.containsKey(name)) return;

        MblWeakArrayList<MblObserver> observers = null;

        observers = mObserverMap.get(name);
        if(!observers.contains(observer)) return;

        observers.remove(observer);

        if(observers.isEmpty()) {
            mObserverMap.remove(name);
        }
    }

    public static void removeAllObserver(MblObserver observer) {
        Set<String> keys = mObserverMap.keySet();
        for (String aKey : keys) {
            removeObserver(observer, aKey);
        }
    }

    public static void postNotification(Object sender, String name, final Object... args) {
        postNotification(sender, true, name, args);
    }

    public static void postNotification(final Object sender, final boolean async, final String name, final Object... args) {
        if(!mObserverMap.containsKey(name)) return;

        MblWeakArrayList<MblObserver> observers;
        if (async) {
            observers = mObserverMap.get(name);
        } else {
            observers = new MblWeakArrayList<MblObserver>(mObserverMap.get(name));
        }
        observers.iterateWithCallback(new MlWeakArrayListCallback<MblObserver>() {
            @Override
            public void onInterate(final MblObserver observer) {
                if (async) {
                    sMainThread.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onNotify(sender, name, args);
                        }
                    });
                } else {
                    observer.onNotify(sender, name, args);
                }
            }
        });
    }
}
