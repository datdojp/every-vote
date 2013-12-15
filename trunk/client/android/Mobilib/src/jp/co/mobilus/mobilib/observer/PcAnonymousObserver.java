package jp.co.mobilus.mobilib.observer;

import java.util.HashSet;
import java.util.Set;

public abstract class PcAnonymousObserver implements PcObserver {

    private static final Set<PcAnonymousObserver> sAnonymousObservers = new HashSet<PcAnonymousObserver>();

    public PcAnonymousObserver() {
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.add(this);
        }
    }

    public void terminate() {
        PcNotificationCenter.removeAllObserver(this);
        synchronized (sAnonymousObservers) {
            sAnonymousObservers.remove(this);
        }
    }
}
