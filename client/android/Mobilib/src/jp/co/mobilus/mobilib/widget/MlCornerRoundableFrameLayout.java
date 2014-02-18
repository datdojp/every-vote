package jp.co.mobilus.mobilib.widget;

import jp.co.mobilus.mobilib.R;
import jp.co.mobilus.mobilib.util.MlUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class MlCornerRoundableFrameLayout extends FrameLayout {
    private int mCornerRadius;
    private Path mClipPath;

    public MlCornerRoundableFrameLayout(Context context) {
        super(context);
    }

    public MlCornerRoundableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MlCornerRoundableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @SuppressLint("NewApi")
    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.MlCornerRoundableFrameLayout, 0, 0);
        int cornerRadius = ta.getInt(R.styleable.MlCornerRoundableFrameLayout_cornerRadius, 0);
        mCornerRadius = MlUtils.pxFromDp(cornerRadius);

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.clipPath(getClipPath());
        super.dispatchDraw(canvas);
    }

    private Path getClipPath() {
        if (mClipPath == null) {
            mClipPath = new Path();
            mClipPath.addRoundRect(new RectF(0, 0, getWidth(), getHeight()),
                    mCornerRadius, mCornerRadius, Direction.CW);
        }
        return mClipPath;
    }

}