package jp.co.mobilus.mobilib.event;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.mobilus.mobilib.event.MblWeakArrayList.MlWeakArrayListCallback;
import android.os.Handler;
import android.os.Looper;

public class MblEventCenter {
    private static final Map<String, MblWeakArrayList<MblEventListener>> mObserverMap = new ConcurrentHashMap<String, MblWeakArrayList<MblEventListener>>();
    private static final Handler sMainThread = new Handler(Looper.getMainLooper());

    private MblEventCenter() {}

    public static void addObserver(MblEventListener observer, String name) {
        MblWeakArrayList<MblEventListener> observers = null;

        if(mObserverMap.containsKey(name)) {
            observers = mObserverMap.get(name);
        } else {
            observers = new MblWeakArrayList<MblEventListener>();
            mObserverMap.put(name, observers);
        }
        if(observers.contains(observer)) return;

        observers.add(observer);
    }

    public static void removeObserver(MblEventListener observer, String name) {
        if(!mObserverMap.containsKey(name)) return;

        MblWeakArrayList<MblEventListener> observers = null;

        observers = mObserverMap.get(name);
        if(!observers.contains(observer)) return;

        observers.remove(observer);

        if(observers.isEmpty()) {
            mObserverMap.remove(name);
        }
    }

    public static void removeAllObserver(MblEventListener observer) {
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

        MblWeakArrayList<MblEventListener> observers;
        if (async) {
            observers = mObserverMap.get(name);
        } else {
            observers = new MblWeakArrayList<MblEventListener>(mObserverMap.get(name));
        }
        observers.iterateWithCallback(new MlWeakArrayListCallback<MblEventListener>() {
            @Override
            public void onInterate(final MblEventListener observer) {
                if (async) {
                    sMainThread.post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onEvent(sender, name, args);
                        }
                    });
                } else {
                    observer.onEvent(sender, name, args);
                }
            }
        });
    }
}
