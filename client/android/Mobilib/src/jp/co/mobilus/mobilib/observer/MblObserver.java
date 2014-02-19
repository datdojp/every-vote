package jp.co.mobilus.mobilib.observer;


public interface MblObserver {
    public void onNotify(Object sender, String name, Object... args);
}
