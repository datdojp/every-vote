package jp.co.mobilus.mobilib.widget;

import jp.co.mobilus.mobilib.R;
import jp.co.mobilus.mobilib.util.MlInternal;
import jp.co.mobilus.mobilib.util.MlUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

public class MlSwitch extends FrameLayout {
    private static final int VERTICAL_PADDING_IN_DP = 2;
    private static final String DEFAULT_ON_TEXT = "On";
    private static final String DEFAULT_OFF_TEXT = "Off";
    private static final boolean DEFAULT_IS_ON = false;

    private static final int ON_COLOR = 0xffa7e330;
    private static final int OFF_COLOR = 0xffef101b;

    private static final int DIRECTION_LEFT_TO_RIGHT = -1;
    private static final int DIRECTION_RIGHT_TO_LEFT = 1;

    private boolean mIsOn = DEFAULT_IS_ON;
    private String mOnText = DEFAULT_ON_TEXT;
    private String mOffText = DEFAULT_OFF_TEXT;
    private Scroller mScroller;
    private GestureDetector mGestureDetector;
    private float mCurrentX;
    private boolean mFlingDetected;
    private int mFlingDirection;
    private boolean mDragging;
    private boolean mSingleTapDetected;
    private FrameLayout mLayoutBehind;
    private View mKnobView;
    private TextView mOnTextView;
    private TextView mOffTextView;
    private LinearLayout mLayoutOfTexts;
    private PCSwitchCallback mCallback;
    private boolean mInitialized;

    public MlSwitch(Context context) {
        super(context);
        initViews(context);
    }

    public MlSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        initViews(context);
    }

    public MlSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttr(context, attrs);
        initViews(context);
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MlSwitch, 0, 0);

        mOnText = ta.getString(R.styleable.MlSwitch_onText);
        if (mOnText == null) mOnText = DEFAULT_ON_TEXT;

        mOffText = ta.getString(R.styleable.MlSwitch_offText);
        if (mOffText == null) mOffText = DEFAULT_OFF_TEXT;

        mIsOn = ta.getBoolean(R.styleable.MlSwitch_isOn, DEFAULT_IS_ON);
    }

    @SuppressLint("NewApi")
    private void initViews(final Context context) {
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MlUtils.removeOnGlobalLayoutListener(MlSwitch.this, this);

                // create layout behind
                final int verticalPadding = MlUtils.pxFromDp(VERTICAL_PADDING_IN_DP);
                final int horizontalPadding = MlUtils.pxFromDp(2);
                final int widthOfLayoutBehind = getWidth();
                final int heightOfLayoutBehind = getHeight() - 2 * verticalPadding;
                final int radius = heightOfLayoutBehind/2;
                final RectF leftArcRect = new RectF(
                        horizontalPadding,
                        0,
                        2 * radius + horizontalPadding,
                        heightOfLayoutBehind);
                final RectF rightArcRect = new RectF(
                        widthOfLayoutBehind - 2 * radius - horizontalPadding, 
                        0,
                        widthOfLayoutBehind - horizontalPadding,
                        heightOfLayoutBehind);
                mLayoutBehind = new FrameLayout(context) {
                    private Path mClipPath;

                    private Path getClipPath() {
                        if (mClipPath == null) {
                            mClipPath = new Path();

                            mClipPath.moveTo(radius + horizontalPadding, heightOfLayoutBehind);
                            mClipPath.arcTo(leftArcRect, 90, 180);
                            mClipPath.lineTo(widthOfLayoutBehind - radius - horizontalPadding, 0);
                            mClipPath.arcTo(rightArcRect, -90, 180);
                            mClipPath.close();
                        }

                        return mClipPath;
                    }

                    @Override
                    protected void dispatchDraw(Canvas canvas) {
                        canvas.clipPath(getClipPath());
                        super.dispatchDraw(canvas);
                    }
                };
                if (Build.VERSION.SDK_INT >= 11) {
                    mLayoutBehind.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
                FrameLayout.LayoutParams lpOfLayoutBehind = new FrameLayout.LayoutParams(widthOfLayoutBehind, heightOfLayoutBehind);
                lpOfLayoutBehind.topMargin = verticalPadding;
                lpOfLayoutBehind.gravity = Gravity.TOP | Gravity.LEFT;
                mLayoutBehind.setLayoutParams(lpOfLayoutBehind);
                mLayoutBehind.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return false; // do not handle touch events
                    }
                });
                addView(mLayoutBehind);

                // create layout to anti alias
                View layoutAntiAlias = new View(context) {
                    private static final int DEGREE_PADDING = 5;

                    @SuppressLint("DrawAllocation")
                    @Override
                    protected void onDraw(Canvas canvas) {
                        super.onDraw(canvas);

                        // draw arc for ON
                        Paint onColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        onColorPaint.setAntiAlias(true);
                        onColorPaint.setColor(ON_COLOR);
                        onColorPaint.setStyle(Style.STROKE);
                        onColorPaint.setStrokeWidth(1);
                        canvas.drawArc(leftArcRect, 90 + DEGREE_PADDING, 180 - DEGREE_PADDING, true, onColorPaint);

                        // draw arc for OFF
                        Paint offColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        offColorPaint.setAntiAlias(true);
                        offColorPaint.setColor(OFF_COLOR);
                        offColorPaint.setStyle(Style.STROKE);
                        offColorPaint.setStrokeWidth(1);
                        canvas.drawArc(rightArcRect, -90 + DEGREE_PADDING, 180 - DEGREE_PADDING, true, offColorPaint);

                    }
                };
                FrameLayout.LayoutParams lpOfLayoutAntiAlias = new FrameLayout.LayoutParams(widthOfLayoutBehind, heightOfLayoutBehind);
                lpOfLayoutAntiAlias.topMargin = verticalPadding;
                lpOfLayoutAntiAlias.gravity = Gravity.TOP | Gravity.LEFT;
                layoutAntiAlias.setLayoutParams(lpOfLayoutAntiAlias);
                addView(layoutAntiAlias, 0);


                // create knob view
                final int widthOfKnobView = getHeight();
                final int heightOfKnobView = getHeight();
                mKnobView = new View(context) {
                    private static final int COLOR = 0xffd3d3d3;
                    private static final int COLOR_PRESSED = 0xffe4e4e4;
                    private Paint mBgPaint;

                    private Paint getBgPaint() {
                        if (mBgPaint == null) {
                            mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            mBgPaint.setStyle(Style.FILL);
                            mBgPaint.setAntiAlias(true);
                        }
                        mBgPaint.setColor(mDragging ? COLOR_PRESSED : COLOR);
                        return mBgPaint;
                    }

                    @Override
                    protected void onDraw(Canvas canvas) {
                        super.onDraw(canvas);
                        canvas.drawCircle(
                                widthOfKnobView/2,
                                heightOfKnobView/2,
                                Math.min(widthOfKnobView, heightOfKnobView)/2,
                                getBgPaint());
                    }
                };
                FrameLayout.LayoutParams lpOfKnobView = new FrameLayout.LayoutParams(widthOfKnobView, heightOfKnobView);
                lpOfKnobView.gravity = Gravity.LEFT | Gravity.TOP;
                mKnobView.setLayoutParams(lpOfKnobView);
                mKnobView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return false; // do not handle touch events
                    }
                });
                addView(mKnobView);

                // add text
                final int widthOfTextView = widthOfLayoutBehind - widthOfKnobView + widthOfKnobView/2;
                final int heighOfTextView = heightOfLayoutBehind;
                int majorTextPadding = widthOfKnobView/2 + MlUtils.pxFromDp(3);
                int minorTextPadding = MlUtils.pxFromDp(3) + horizontalPadding + heightOfLayoutBehind/4;
                mOnTextView = generateTextView(context, mOnText, widthOfTextView, heighOfTextView, ON_COLOR,
                        minorTextPadding, majorTextPadding);
                mOffTextView = generateTextView(context, mOffText, widthOfTextView, heighOfTextView, OFF_COLOR,
                        majorTextPadding, minorTextPadding);

                mLayoutOfTexts = new LinearLayout(context);
                FrameLayout.LayoutParams lpOfLayoutOfTexts = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                lpOfLayoutOfTexts.gravity = Gravity.LEFT | Gravity.TOP;
                mLayoutOfTexts.setLayoutParams(lpOfLayoutOfTexts);
                mLayoutOfTexts.setOrientation(LinearLayout.HORIZONTAL);

                mLayoutOfTexts.addView(mOnTextView);
                mLayoutOfTexts.addView(mOffTextView);
                mLayoutBehind.addView(mLayoutOfTexts);

                // scroller and gesture recognizer
                mScroller = new Scroller(context);
                mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        mDragging = true;
                        mFlingDetected = false;
                        mSingleTapDetected = false;

                        mKnobView.invalidate(); // redraw knob view 's background

                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        if (mDragging) {
                            float toX = mCurrentX - distanceX;
                            toX = Math.max(toX, getMostLeftX());
                            toX = Math.min(toX, getMostRightX());
                            scrollTo(toX);
                        }
                        return true;
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        mSingleTapDetected = true;
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        mFlingDetected = true;
                        mFlingDirection = velocityX > 0 ? DIRECTION_LEFT_TO_RIGHT : DIRECTION_RIGHT_TO_LEFT;

                        mScroller.fling(
                                (int) mCurrentX, 0,
                                (int) -velocityX, 0,
                                getMostLeftX(), getMostRightX(),
                                0, 0);
                        updateTranslationX();

                        return true;
                    }
                });

                // initialization is done
                mInitialized = true;

                // set initial status
                MlInternal.getMainThread().post(new Runnable() {
                    @Override
                    public void run() {
                        setOn(mIsOn);
                    }
                });
            }
        });
    }

    private void scrollTo(float toX) {
        mScroller.startScroll((int) mCurrentX, 0, (int)(toX - mCurrentX), 0);
        mCurrentX = toX;
        updateTranslationX();
    }

    private void updateTranslationX() {
        if (mScroller.computeScrollOffset()) {
            setLeftMargin(mKnobView, mScroller.getCurrX());
            setLeftMargin(mLayoutOfTexts, getCurrentXOfLayoutOfTexts(mScroller.getCurrX()));
        }
        if (!mScroller.isFinished()) {
            MlInternal.getMainThread().post(new Runnable() {
                @Override
                public void run() {
                    updateTranslationX();
                }
            });
        } else {
            mCurrentX = mScroller.getCurrX();
        }
    }

    private TextView generateTextView(Context context, String text, int width, int height, int backgroundColor,
            int paddingLeft, int paddingRight) {
        TextView textView = new TextView(context);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        textView.setLayoutParams(lp);

        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        textView.setEllipsize(TruncateAt.END);
        textView.setPadding(paddingLeft, 0, paddingRight, 0);
        textView.setBackgroundColor(backgroundColor);

        textView.setTextSize(14);
        textView.setTextColor(0xffffffff);
        textView.setText(text);

        return textView;
    }

    public void setOn(boolean isOn) {
        if (mInitialized) {
            mCurrentX = isOn ? getMostRightX() : getMostLeftX();
            setLeftMargin(mLayoutOfTexts, getCurrentXOfLayoutOfTexts(mCurrentX));
            setLeftMargin(mKnobView, (int) mCurrentX);
        }
        mIsOn = isOn;
    }

    private int getCurrentXOfLayoutOfTexts(float currentX) {
        return (int) (currentX - mOnTextView.getWidth() + mKnobView.getWidth()/2);
    }

    private void setLeftMargin(View view, int leftMargin) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        lp.leftMargin = leftMargin;
        view.setLayoutParams(lp);
    }

    public boolean isOn() {
        return mIsOn;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (mFlingDetected) {
                handleFling();
            } else if (mSingleTapDetected) {
                handleSingleTap();
            } else {
                handleFinishDragging();
            }
            mFlingDetected = false;
            mDragging = false;
            mSingleTapDetected  = false;

            mKnobView.invalidate(); // redraw knob view 's background
        }
        return true;
    }

    private void handleFinishDragging() {
        float relativeCurrentX = mCurrentX + mKnobView.getWidth()/2;
        boolean newOnStatus;
        if (relativeCurrentX > getWidth() / 2) {
            scrollTo(getMostRightX());
            newOnStatus = true;
        } else {
            scrollTo(getMostLeftX());
            newOnStatus = false;
        }
        if (mCallback != null && mIsOn != newOnStatus) {
            mCallback.onStatusChanged(newOnStatus);
        }
        mIsOn = newOnStatus;
    }

    private void handleFling() {
        boolean newOnStatus = mIsOn;
        if (mFlingDirection == DIRECTION_LEFT_TO_RIGHT) {
            scrollTo(getMostRightX());
            newOnStatus = true;
        } else if (mFlingDirection == DIRECTION_RIGHT_TO_LEFT) {
            scrollTo(getMostLeftX());
            newOnStatus = false;
        }
        if (mCallback != null && mIsOn != newOnStatus) {
            mCallback.onStatusChanged(newOnStatus);
        }
        mIsOn = newOnStatus;
    }

    private void handleSingleTap() {
        boolean newOnStatus;
        if (mIsOn) {
            scrollTo(getMostLeftX());
            newOnStatus = false;
        } else {
            scrollTo(getMostRightX());
            newOnStatus = true;
        }
        if (mCallback != null && mIsOn != newOnStatus) {
            mCallback.onStatusChanged(newOnStatus);
        }
        mIsOn = newOnStatus;
    }

    private int getMostLeftX() {
        return 0;
    }

    private int getMostRightX() {
        return getWidth() - mKnobView.getWidth();
    }

    public static interface PCSwitchCallback {
        public void onStatusChanged(boolean newOnStatus);
    }

    public void setCallback(PCSwitchCallback callback) {
        mCallback = callback;
    }
}
