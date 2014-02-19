package jp.co.mobilus.mobilib.observer;

import java.util.HashSet;
import java.util.Set;

public abstract class MblAnonymousObserver implements MblObserver {

    private static final Set<MblAnonymousObserver> sAnonymousObservers = new HashSet<MblAnonymousObserver>();

    public MblAnonymousObserver() {
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.add(this);
        }
    }

    public void terminate() {
        MblNotificationCenter.removeAllObserver(this);
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.remove(this);
        }
    }
}
