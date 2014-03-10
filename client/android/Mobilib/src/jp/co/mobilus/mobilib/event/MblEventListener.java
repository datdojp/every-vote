package jp.co.mobilus.mobilib.event;


public interface MblEventListener {
    public void onEvent(Object sender, String name, Object... args);
}
