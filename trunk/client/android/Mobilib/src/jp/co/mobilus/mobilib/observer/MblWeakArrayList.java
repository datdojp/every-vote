package jp.co.mobilus.mobilib.observer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

class MblWeakArrayList<T> {
    private List<WeakReference<T>> mData = new Vector<WeakReference<T>>();

    public MblWeakArrayList() {}

    public MblWeakArrayList(MblWeakArrayList<T> other) {
        mData.clear();
        mData.addAll(other.mData);
    }

    public void add(T item) {
        if (item == null) return;

        synchronized (mData) {
            if (!intContains(item)) {
                mData.add(new WeakReference<T>(item));
            }
        }
    }

    public void remove(T item) {
        if (item == null) return;

        synchronized (mData) {
            int index = intIndexOf(item);
            if (index >= 0) {
                mData.remove(index);
            }
        }
    }

    public boolean contains(T item) {
        synchronized (mData) {
            return intContains(item);
        }
    }

    public boolean isEmpty() {
        synchronized (mData) {
            intFlush();
            return mData.isEmpty();
        }
    }

    public void iterateWithCallback(MlWeakArrayListCallback<T> cb) {
        if (cb == null) return;

        synchronized (mData) {
            intFlush();
            for (WeakReference<T> ref : mData) {
                T item = ref.get();
                if (item != null) cb.onInterate(item);
            }
        }
    }

    @Deprecated
    public Iterator<T> iterate() {
        return null; // not yet used
    }

    public static interface MlWeakArrayListCallback<T> {
        public void onInterate(T item);
    }

    private boolean intContains(T item) {
        return intIndexOf(item) >= 0;
    }

    private int intIndexOf(T item) {
        if (item == null) return -1;

        int i = 0;
        for (WeakReference<T> anItem : mData) {
            if (anItem.get() == item) {
                return i;
            }
            i++;
        }

        return -1;
    }

    private void intFlush() {
        List<WeakReference<T>> needToRemove = new ArrayList<WeakReference<T>>();
        for (WeakReference<T> anItem : mData) {
            if (anItem.get() == null) {
                needToRemove.add(anItem);
            }
        }
        mData.removeAll(needToRemove);
    }
}