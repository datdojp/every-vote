package jp.co.mobilus.mobilib.observer;


public interface MlObserver {
    public void onNotify(Object sender, String name, Object... args);
}
