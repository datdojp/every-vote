package jp.co.mobilus.mobilib.observer;


public interface PcObserver {
    public void onNotify(Object sender, String name, Object... args);
}
