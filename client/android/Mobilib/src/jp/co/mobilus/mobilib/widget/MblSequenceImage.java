package jp.co.mobilus.mobilib.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;

public class MblSequenceImage extends ImageView {
    private int mCurrentIndex;
    private int[] mImageResIds;
    private long mInterval;
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private Runnable mOnTimerTask = new Runnable() {
        @Override
        public void run() {
            setImageResource(mImageResIds[mCurrentIndex]);
            mCurrentIndex = (mCurrentIndex+1) % mImageResIds.length;

            sHandler.removeCallbacks(mOnTimerTask);
            sHandler.postDelayed(mOnTimerTask, mInterval);
        }
    };

    public MblSequenceImage(Context context) {
        super(context);
    }

    public MblSequenceImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MblSequenceImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(int[] imageResIds, long interval) {
        mImageResIds = imageResIds;
        mInterval = interval;
        mCurrentIndex = 0;
    }

    public void start() {
        stop();
        mCurrentIndex = 0;
        mOnTimerTask.run();
    }

    public void showImageAtIndex(int index) {
        setImageResource(mImageResIds[index]);
        mCurrentIndex = index;
    }
    
    public void stop() {
        sHandler.removeCallbacks(mOnTimerTask);
    }
}
