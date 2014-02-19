package jp.co.mobilus.mobilib.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

public abstract class MblDataLoader<T> {
    @SuppressWarnings("unused")
    private static final String TAG = MblUtils.getTag(MblDataLoader.class);

    public static interface DataRetrievedCallback<T> {
        public void onRetrieved(T data);
    }

    /**
     * methods needed to be implemented by subclasses
     */
    protected abstract ViewGroup getContainerLayout();
    protected abstract boolean shouldLoadOneByOne();
    protected abstract T getItemFromView(View view);
    protected abstract boolean shouldLoadDataForItem(T item);
    protected abstract void retrieveData(T item, DataRetrievedCallback<T> cb);
    protected abstract boolean matchViewAndItem(View view, T item);
    protected abstract void updateView(View view);
    protected abstract void applyDataOnItem(T item, T data);

    /**
     * instance variables
     */
    private final Stack<T> mQueue = new Stack<T>();
    private final Set<T> mItemsBeingLoaded = new HashSet<T>();
    private LoadOrder mLoadOrder = LoadOrder.FIRST_TO_LAST;

    /**
     * class variables and class methods
     */
    private static final Handler mMainThreadHandler = new Handler();

    /**
     * constructors
     */
    public MblDataLoader() {
        this(LoadOrder.FIRST_TO_LAST);
    }
    public MblDataLoader(LoadOrder loadOrder) {
        mLoadOrder = loadOrder;
    }

    /**
     * This methods should be called when the view did disappear
     */
    public void stop() {
        mQueue.clear();
    }

    public void loadData(final View view) {
        T item = getItemFromView(view);
        if (item == null) return;
        updateView(view); // show current data
        if (shouldLoadDataForItem(item)) { // load data if needed
            mQueue.push(item);
            loadNextData();
        }
    }

    private T getNextItem() {
        ViewGroup viewGroup = getContainerLayout();
        if (viewGroup == null) {
            stop();
            return null;
        }
        if (mQueue.isEmpty()) return null;

        // find the next visible item in queue
        if (mLoadOrder == LoadOrder.FIRST_TO_LAST) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                T item = doGetNextItem(viewGroup, i);
                if (item != null) return item;
            }
        } else if (mLoadOrder == LoadOrder.LAST_TO_FIRST) {
            for (int i = viewGroup.getChildCount()-1; i >= 0; i--) {
                T item = doGetNextItem(viewGroup, i);
                if (item != null) return item;
            }
        }

        // no visible item in queue, get item on the top of queue to load
        return mQueue.pop();
    }

    private T doGetNextItem(ViewGroup viewGroup, int i) {
        View view = viewGroup.getChildAt(i);
        T item = getItemFromView(view);
        if (item != null) {
            if (mQueue.contains(item)) {
                mQueue.remove(item);
                return item;
            }
        }
        return null;
    }

    private void loadNextData() {
        if (mQueue.isEmpty()) return;
        if (shouldLoadOneByOne() && !mItemsBeingLoaded.isEmpty()) return;

        final T item = getNextItem();
        if (item == null) return;

        if (!shouldLoadDataForItem(item)) { // item is already loaded in previous loading
            postRetrieveData(item, item);
            return;
        }
        
        if (!shouldLoadOneByOne() && mItemsBeingLoaded.contains(item)) {
            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadNextData();
                }
            });
            return;
        }

        mItemsBeingLoaded.add(item);
        retrieveData(item, new DataRetrievedCallback<T>() {
            @Override
            public void onRetrieved(T data) {
                if (data != null)  {
                    postRetrieveData(item, data);
                } else {
                    mMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (mItemsBeingLoaded) {
                                mItemsBeingLoaded.remove(item);
                            }
                            loadNextData();
                        }
                    });
                }
            }
        });
    }

    private void postRetrieveData(final T item, final T data) {
        applyDataOnItem(item, data);

        // update views
        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup viewGroup = getContainerLayout();
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View view = viewGroup.getChildAt(i);
                    if (matchViewAndItem(view, item)) {
                        updateView(view);
                    }
                }
                synchronized (mItemsBeingLoaded) {
                    mItemsBeingLoaded.remove(item);
                }
                loadNextData();
            }
        });
    }

    public static enum LoadOrder {
        FIRST_TO_LAST,
        LAST_TO_FIRST;
    }
}
