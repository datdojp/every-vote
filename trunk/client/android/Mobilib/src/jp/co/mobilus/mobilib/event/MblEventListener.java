package jp.co.mobilus.mobilib.event;


public interface MblEventListener {
    public void onNotify(Object sender, String name, Object... args);
}
