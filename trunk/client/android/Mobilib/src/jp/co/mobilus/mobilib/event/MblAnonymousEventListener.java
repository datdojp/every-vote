package jp.co.mobilus.mobilib.event;

import java.util.HashSet;
import java.util.Set;

public abstract class MblAnonymousEventListener implements MblEventListener {

    private static final Set<MblAnonymousEventListener> sAnonymousObservers = new HashSet<MblAnonymousEventListener>();

    public MblAnonymousEventListener() {
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.add(this);
        }
    }

    public void terminate() {
        MblEventCenter.removeAllObserver(this);
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.remove(this);
        }
    }
}
