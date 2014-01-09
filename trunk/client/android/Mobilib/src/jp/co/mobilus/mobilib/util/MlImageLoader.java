package jp.co.mobilus.mobilib.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public abstract class MlImageLoader<T> {
    private static final String TAG = MlUtils.getTag(MlImageLoader.class);
    private static final int DEFAULT_CACHE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String CACHE_KEY_SEPARATOR = "#";

    /**
     * methods needed to be implemented by subclasses
     */
    protected abstract ViewGroup getContainerLayout();
    protected abstract int getImageWidth();
    protected abstract int getImageHeight();
    protected abstract String getItemPrefix();
    protected abstract String getItemImageId(T item);
    protected abstract int getDefaultImageResource(T item);
    protected int getErrorImageResource(T item) {
        return getDefaultImageResource(item);
    }
    protected abstract boolean shouldLoadImage(T item);
    protected abstract boolean matchViewAndItem(View view, T item);
    protected abstract ImageView getImageView(View view);
    protected abstract T getItem(View view);
    protected abstract void retrieveImage(T item, boolean isCacheEnabled, ImageRetrievingCallback cb);
    protected static interface ImageRetrievingCallback {
        public void onRetrievedByteArrray(byte[] bmData);
        public void onRetrievedBitmap(Bitmap bm);
    }

    /**
     * instance variables
     */
    private final Stack<T> mQueue = new Stack<T>();
    private boolean isLoadingImage = false;
    private MlImageLoaderLoadOrder mLoadOrder = MlImageLoaderLoadOrder.FIRST_TO_LAST;

    /**
     * class variables and class methods
     */
    private static final Handler sMainThreadHandler = new Handler();
    private static LruCache<String, Object> mStringPictureLruCache;
    private static final Set<String> mCacheKeys = new HashSet<String>();
    private static final Set<String> mCacheKeyPrefixesNeedToRefresh = new HashSet<String>();
    private static final Set<WeakReference<ImageView>> mLoadedImageViews = new HashSet<WeakReference<ImageView>>();
    private static void initCacheIfNeeded() {
        if (mStringPictureLruCache == null) {
            Context context = MlInternal.getInstance() != null ? MlInternal.getInstance().getCurrentContext() : null;
            int cacheSize = DEFAULT_CACHE_SIZE;
            if (context != null) {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
                cacheSize = memoryClassBytes / 8;
            }
            mStringPictureLruCache = new LruCache<String, Object>(cacheSize) {
                @Override
                protected void entryRemoved(boolean evicted, String key, Object oldValue, Object newValue) {
                    if (oldValue instanceof Bitmap) {
                        unloadImageViewBoundWithBitmap((Bitmap) oldValue);
                    }
                    synchronized (mCacheKeys) {
                        mCacheKeys.remove(key);
                    }
                    Log.v(TAG, "Image cache size: " + size());
                }

                @Override
                protected int sizeOf(String key, Object value) {
                    if (value instanceof Bitmap) {
                        Bitmap bm = (Bitmap) value;
                        return bm.getRowBytes() * bm.getHeight();
                    } else if (value instanceof Integer) {
                        return 4;
                    }
                    return 0;
                }
            };
        }
    }
    private static void remove(String key) {
        initCacheIfNeeded();
        synchronized (mStringPictureLruCache) {
            mStringPictureLruCache.remove(key);
        }
    }
    private static void removePrefix(String prefix) {
        initCacheIfNeeded();
        synchronized (mStringPictureLruCache) {
            List<String> keysWillBeDeleted = new ArrayList<String>();
            synchronized (mCacheKeys) {
                for (String key : mCacheKeys) {
                    if (key.startsWith(prefix)) {
                        keysWillBeDeleted.add(key);
                    }
                }
            }
            for (String key : keysWillBeDeleted) {
                mStringPictureLruCache.remove(key);
            }
        }
    }
    private static void put(String key, Object val) {
        initCacheIfNeeded();
        synchronized (mStringPictureLruCache) {
            synchronized (mCacheKeys) {
                mStringPictureLruCache.put(key, val);
                mCacheKeys.add(key);
                Log.v(TAG, "Image cache size: " + mStringPictureLruCache.size());
            }
        }
    }
    private static Object get(String key) {
        initCacheIfNeeded();
        return mStringPictureLruCache.get(key);
    }
    private static void storeLoadedImageView(ImageView imageView) {
        if (imageView == null) return;
        synchronized (mLoadedImageViews) {
            boolean found = false;
            for (WeakReference<ImageView> wref : mLoadedImageViews) {
                if (wref.get() == imageView) {
                    found = true;
                    break;
                }
            }
            if (!found) mLoadedImageViews.add(new WeakReference<ImageView>(imageView));
        }
    }
    private static void unloadImageViewBoundWithBitmap(Bitmap bm) {
        if (bm == null) return;
        synchronized (mLoadedImageViews) {
            List<WeakReference<ImageView>> needToRemove = new ArrayList<WeakReference<ImageView>>();
            final List<ImageView> needToUnload = new ArrayList<ImageView>();
            for (WeakReference<ImageView> wref : mLoadedImageViews) {
                ImageView imageView = wref.get();
                if (imageView != null) {
                    Drawable drawable = imageView.getDrawable();
                    if (drawable != null && drawable instanceof BitmapDrawable 
                            && ((BitmapDrawable)drawable).getBitmap() == bm) {
                        needToUnload.add(imageView);
                    }
                } else {
                    needToRemove.add(wref);
                }
            }
            mLoadedImageViews.removeAll(needToRemove);
            sMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (ImageView iv : needToUnload) {
                        iv.setImageBitmap(null);
                    }
                }
            });
        }
    }

    public MlImageLoader() {
        this(MlImageLoaderLoadOrder.FIRST_TO_LAST);
    }

    public MlImageLoader(MlImageLoaderLoadOrder loadOrder) {
        mLoadOrder = loadOrder;
    }

    /**
     * This methods should be called when the view did disappear
     */
    public void stop() {
        mQueue.clear();
    }

    public void loadImage(View view) {
        T item = getItem(view);
        ImageView imageView = getImageView(view);
        if (item == null || imageView == null) return;
        if (!shouldLoadImage(item)) {
            onPictureUnavailable(view, imageView, item);
            imageView.setImageResource(getDefaultImageResource(item));
            return;
        }
        final Object pic = get(getFullCacheKey(item));
        if(pic != null) {
            if (pic instanceof Bitmap) {
                Bitmap bm = (Bitmap) pic;
                if (!bm.isRecycled()) {
                    onPictureAvailable(view, imageView, item, pic);
                    imageView.setImageBitmap(bm);
                    storeLoadedImageView(imageView);
                } else {
                    onPictureUnavailable(view, imageView, item);
                    remove(getFullCacheKey(item));
                    handleBitmapUnavailable(imageView, item);
                }
            } else if (pic instanceof Integer) {
                onPictureAvailable(view, imageView, item, pic);
                imageView.setImageResource((Integer) pic);
            }
        } else {
            onPictureUnavailable(view, imageView, item);
            handleBitmapUnavailable(imageView, item);
        }
    }

    protected void onPictureAvailable(View view, ImageView imageView, T item, Object pic) {}
    protected void onPictureUnavailable(View view, ImageView imageView, T item) {}

    private void handleBitmapUnavailable(ImageView imageView, T item) {
        imageView.setImageResource(getDefaultImageResource(item));
        mQueue.push(item);
        loadNextImage();
    }

    private boolean shouldEnableCache(T item) {
        String prefix = getCacheKeyPrefix(item);
        if (mCacheKeyPrefixesNeedToRefresh.contains(prefix)) {
            synchronized (mCacheKeyPrefixesNeedToRefresh) {
                mCacheKeyPrefixesNeedToRefresh.remove(prefix);
            }
            return false;
        }
        return true;
    }

    private T getNextItem() {
        ViewGroup viewGroup = getContainerLayout();
        if (viewGroup == null) {
            stop();
            return null;
        }
        if (mQueue.isEmpty()) return null;

        // find the next visible item in queue
        if (mLoadOrder == MlImageLoaderLoadOrder.FIRST_TO_LAST) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                T item = doGetNextItem(viewGroup, i);
                if (item != null) return item;
            }
        } else if (mLoadOrder == MlImageLoaderLoadOrder.LAST_TO_FIRST) {
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
        T item = getItem(view);
        if (item != null) {
            if (mQueue.contains(item)) {
                mQueue.remove(item);
                return item;
            }
        }
        return null;
    }

    private void loadNextImage() {
        if (mQueue.isEmpty()) return;
        if (isLoadingImage) return;

        final T item = getNextItem();
        if (item == null) return;

        if (!shouldLoadImage(item)
                || get(getFullCacheKey(item)) != null) {
            sMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadNextImage();					
                }
            });
            return;
        }

        isLoadingImage = true;
        boolean shouldEnableCache = shouldEnableCache(item);
        final boolean isNetworkConnected = MlUtils.isNetworkConnected();
        retrieveImage(item, shouldEnableCache, new ImageRetrievingCallback() {
            @Override
            public void onRetrievedByteArrray(final byte[] bmData) {
                if (bmData == null || bmData.length == 0) {
                    handleBadReturnedBitmap(item, isNetworkConnected);
                } else {
                    new MlAsyncTask() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                Bitmap bm;
                                if (getImageWidth() > 0 || getImageHeight() > 0) {
                                    bm = MlUtils.loadBitmapMatchSpecifiedSize(getImageWidth(), getImageHeight(), bmData);
                                } else {
                                    bm = BitmapFactory.decodeByteArray(bmData, 0, bmData.length);
                                }
                                if (bm == null) {
                                    handleBadReturnedBitmap(item, isNetworkConnected);
                                } else {
                                    handleGoodReturnedBitmap(item, bm);
                                }
                            } catch (OutOfMemoryError e) {
                                Log.e(TAG, "OutOfMemoryError", e);
                                handleOutOfMemory(item);
                            }
                            return null;
                        }
                    }.execute();
                }
            }

            @Override
            public void onRetrievedBitmap(Bitmap bm) {
                if (bm == null) {
                    handleBadReturnedBitmap(item, isNetworkConnected);
                } else {
                    handleGoodReturnedBitmap(item, bm);
                }
            }
        });
    }

    private void handleGoodReturnedBitmap(T item, Bitmap bm) {
        put(getFullCacheKey(item), bm);
        postLoadImageForItem(item);
    }

    private void handleBadReturnedBitmap(T item, boolean isNetworkConnected) {
        final String fullCacheKey = getFullCacheKey(item);
        put(fullCacheKey, getErrorImageResource(item));
        postLoadImageForItem(item);
        
        // failed due to network disconnect -> should try to load later
        if (!isNetworkConnected) {
            sMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    remove(fullCacheKey);
                }
            });
        }
    }

    private void handleOutOfMemory(T item) {
        // release 1/2 of cache size for memory
        synchronized (mStringPictureLruCache) {
            mStringPictureLruCache.trimToSize(mStringPictureLruCache.size()/2);
        }
        System.gc();

        // load image again later
        mQueue.add(0, item); // add to bottom of queue
        sMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                isLoadingImage = false;
                loadNextImage();
            }
        });
    }

    private void postLoadImageForItem(final T item) {
        sMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup viewGroup = getContainerLayout();
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View view = viewGroup.getChildAt(i);
                    if (matchViewAndItem(view, item)) loadImage(view);
                }
                isLoadingImage = false;
                loadNextImage();
            }
        });
    }

    private String getFullCacheKey(T item) {
        return TextUtils.join(CACHE_KEY_SEPARATOR, new Object[] {
                getItemPrefix(),
                getItemImageId(item),
                getImageWidth(),
                getImageHeight()
        });
    }

    private String getCacheKeyPrefix(T item) {
        return TextUtils.join(CACHE_KEY_SEPARATOR, new Object[] {
                getItemPrefix(),
                getItemImageId(item)
        });
    }

    public void releaseItem(T item) {
        if (item == null) return;
        String key = getFullCacheKey(item);
        remove(key);
    }

    // will be used in the future
    public static void releaseEverything() {
        removePrefix("");
        System.gc();
    }

    public static enum MlImageLoaderLoadOrder {
        FIRST_TO_LAST,
        LAST_TO_FIRST;
    }

    public synchronized static boolean loadInternalImage(
            final Context context,
            final String path,
            final ImageView target) {

        if (MlUtils.isEmpty(path)) return false;
        String key = getInternalStorageKey(path);
        Bitmap bm = (Bitmap) get(key);
        if (bm != null) {
            if (target != null) {
                final Bitmap temp = bm;
                sMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        target.setImageBitmap(temp);
                        storeLoadedImageView(target);
                    }
                });
            }
        } else {
            try {
                InputStream is = context.openFileInput(path);
                bm = BitmapFactory.decodeStream(is);
                put(key, bm);
                if (target != null) {
                    final Bitmap temp = bm;
                    sMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            target.setImageBitmap(temp);
                            storeLoadedImageView(target);
                        }
                    });
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Can not load image from internal storage: path=" + path, e);
                return false;
            }
        }
        return true;
    }

    public synchronized static boolean releaseInternalImage(Context context, String path) {
        if (MlUtils.isEmpty(path)) return false;
        String key = getInternalStorageKey(path);
        remove(key);
        return true;
    }

    private static String getInternalStorageKey(String path) {
        return "internal_" + path;
    }
}
