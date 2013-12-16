package jp.co.mobilus.mobilib.util;

import java.util.List;

import android.os.Handler;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

public abstract class MlPager<T> {
    /**
     * instance variables
     */
    private int mOffSet;
    private int mLimitPerLoad;
    private boolean mIsLoading;
    private boolean mDidReachLastItem;
    private PcPagerLoadOrder mLoadOrder = PcPagerLoadOrder.LOAD_MORE_AT_BOTTOM;
    private int mPreviousFirstVisibleItem = 0;
    private static final Handler sMainThreadHandler = new Handler();

    /**
     * public methods
     */
    public void reset() {
        mOffSet = 0;
        mDidReachLastItem = false;
        mPreviousFirstVisibleItem = 0;
        mIsLoading = false;
    }

    public void loadMore() {
        if (mIsLoading || mDidReachLastItem) return;

        mIsLoading = true;
        if (shouldShowLoading()) showLoading();
        load(mOffSet, mLimitPerLoad, new PcPagerCallback<T>() {
            @Override
            public void onLoaded(List<T> results) {
                if (shouldShowLoading()) hideLoading();
                mIsLoading = false;
                if (results == null) return;

                if (isPreviousItemsIncludedInLoadedItem()) {
                    if (results.size() < mOffSet + mLimitPerLoad) mDidReachLastItem = true;
                    if (mLoadOrder == PcPagerLoadOrder.LOAD_MORE_AT_TOP) {
                        mPreviousFirstVisibleItem = results.size() - mOffSet;
                    }
                    mOffSet = results.size();
                } else {
                    if (results.size() < mLimitPerLoad) mDidReachLastItem = true;
                    if (mLoadOrder == PcPagerLoadOrder.LOAD_MORE_AT_TOP) {
                        mPreviousFirstVisibleItem = results.size();
                    }
                    mOffSet += results.size();
                }

                processNewItems(results);
                if (mLoadOrder == PcPagerLoadOrder.LOAD_MORE_AT_TOP) {
                    sMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            getListView().setSelection(mPreviousFirstVisibleItem);
                        }
                    });
                }
            }
        });
    }

    protected boolean didReachLastItem() {
        return mDidReachLastItem;
    }

    /**
     * constructors
     */
    public MlPager(int offset, int limitPerLoad, PcPagerLoadOrder loadOrder) {
        this(offset, limitPerLoad);
        mLoadOrder = loadOrder;
    }

    public MlPager(int offset, int limitPerLoad) {
        mOffSet = offset;
        mLimitPerLoad = limitPerLoad;

        getListView().setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                MlPager.this.onScrollStateChanged(view, scrollState);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mLoadOrder == PcPagerLoadOrder.LOAD_MORE_AT_BOTTOM) {
                    if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                        if (isAutoLoadMore()) {
                            loadMore();
                        } else {
                            onNeedLoadMore();
                        }
                    }
                } else if (mLoadOrder == PcPagerLoadOrder.LOAD_MORE_AT_TOP) {
                    if (firstVisibleItem == 0 && mPreviousFirstVisibleItem > firstVisibleItem) {
                        if (isAutoLoadMore()) {
                            loadMore();
                        } else {
                            onNeedLoadMore();
                        }
                    }
                }
                mPreviousFirstVisibleItem = firstVisibleItem;
                MlPager.this.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
        });
    }

    /**
     * methods those need to be implemented/overridden by subclasses
     */
    protected abstract ListView getListView();
    protected abstract boolean shouldShowLoading();
    protected abstract void load(int offset, int limit, PcPagerCallback<T> cb);
    protected abstract void processNewItems(List<T> newItems);
    protected abstract void showLoading();
    protected abstract void hideLoading();
    protected abstract boolean isPreviousItemsIncludedInLoadedItem();
    protected abstract void onScrollStateChanged(AbsListView view, int scrollState);
    protected abstract void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    protected boolean isAutoLoadMore() {
        return true;
    }
    protected void onNeedLoadMore() {
        // to be overridden
    }

    /**
     * inner classes
     */
    public static interface PcPagerCallback<T> {
        public void onLoaded(List<T> results);
    }

    public static enum PcPagerLoadOrder {
        LOAD_MORE_AT_BOTTOM,
        LOAD_MORE_AT_TOP;
    }

    public void updateOffset(int offset) {
        mOffSet = offset;
    }
}
