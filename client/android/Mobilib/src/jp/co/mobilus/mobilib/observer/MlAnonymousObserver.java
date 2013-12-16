package jp.co.mobilus.mobilib.observer;

import java.util.HashSet;
import java.util.Set;

public abstract class MlAnonymousObserver implements MlObserver {

    private static final Set<MlAnonymousObserver> sAnonymousObservers = new HashSet<MlAnonymousObserver>();

    public MlAnonymousObserver() {
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.add(this);
        }
    }

    public void terminate() {
        MlNotificationCenter.removeAllObserver(this);
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.remove(this);
        }
    }
}
