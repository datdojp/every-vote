package jp.co.mobilus.mobilib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.pokelabo.pokechat.PcWebActivity;
import jp.co.pokelabo.pokechat.R;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.github.johnpersano.supertoasts.SuperToast;

public class PcUtils {
    private static final String TAG = getTag(PcUtils.class);
    private static float density = 0;
    private static final String EMAIL_TYPE = "message/rfc822";

    public static void hideKeyboard() {
        Activity activity = (Activity) PcInternal.getInstance().getCurrentContext();
        View currentFocusedView = activity.getCurrentFocus();
        if (currentFocusedView != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), 0);
        }
    }

    @SuppressWarnings("rawtypes")
    public static String getTag(Class c) {
        return c.getSimpleName();
    }

    public static int pxFromDp(int dp) {
        if (density == 0) {
            density = PcInternal.getInstance().getCurrentContext().getResources().getDisplayMetrics().density;
        }
        return (int) (dp * density);
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static boolean isPortraitDisplay() {
        return PcInternal.getInstance().getCurrentContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static boolean isPortraitWindow() {
        Activity activity = (Activity) PcInternal.getInstance().getCurrentContext();
        View root = activity.getWindow().getDecorView();
        return root.getWidth() <= root.getHeight();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static boolean isNetworkConnected() {
        return isNetworkConnected(PcInternal.getInstance().getCurrentContext());
    }

    public static Bitmap loadBitmapMatchSpecifiedSize(final int targetW, final int targetH, final byte[] bmData) {
        int scaleFactor = 1;

        // get image view sizes
        int photoW = 0;
        int photoH = 0;
        if (targetW > 0 || targetH > 0) {
            // get bitmap sizes
            int[] photoSizes = getBitmapSizes(bmData);
            photoW = photoSizes[0];
            photoH = photoSizes[1];

            // figure out which way needs to be reduced less
            if (photoW > 0 && photoH > 0) {
                if (targetW > 0 && targetH > 0) {
                    if ((targetW > targetH && photoW < photoH) || (targetW < targetH && photoW > photoH)) {
                        scaleFactor = Math.max(photoW / targetW, photoH / targetH);
                    } else {
                        scaleFactor = Math.min(photoW / targetW, photoH / targetH);
                    }
                } else if (targetW > 0) {
                    scaleFactor = photoW / targetW;
                } else if (targetH > 0) {
                    scaleFactor = photoH / targetH;
                }
            }
        }

        // set bitmap options to scale the image decode target
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        // decode the bitmap
        Bitmap bm = BitmapFactory.decodeByteArray(bmData, 0, bmData.length, bmOptions);
        return bm;
    }

    public static int[] getBitmapSizes(byte[] bmData) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bmData, 0, bmData.length, bmOptions);
        return new int[]{ bmOptions.outWidth, bmOptions.outHeight };
    }

    public static int[] getBitmapSizes(int resId) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(PcInternal.getInstance().getCurrentContext().getResources(), resId, bmOptions);
        return new int[]{ bmOptions.outWidth, bmOptions.outHeight };
    }

    public static int[] getBitmapSizes(String path) throws IOException {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        FileInputStream is = new FileInputStream(path);
        BitmapFactory.decodeStream(is, null, bmOptions);
        is.close();
        return new int[] { bmOptions.outWidth, bmOptions.outHeight };
    }

    public static boolean recycleBitmap(Bitmap bm) {
        if (bm != null && !bm.isRecycled()) {
            bm.recycle();
            return true;
        }
        return false;
    }

    public static boolean recycleImageView(ImageView imageView) {
        Bitmap bm = extractBitmap(imageView);
        imageView.setImageBitmap(null);
        return recycleBitmap(bm);
    }

    public static Bitmap extractBitmap(ImageView imageView) {
        if (imageView == null) return null;
        Drawable drawable = imageView.getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            Bitmap bm = ((BitmapDrawable)drawable).getBitmap();
            return bm;
        }
        return null;
    }

    /**
     * check if android:debuggable is set to true
     */
    public static boolean getAppFlagDebug(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        int appFlags = appInfo.flags;
        boolean b = (appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        return b;
    }

    public static Date convertUnixtime(long unixtime) {
        return new Date(unixtime * 1000L);
    }

    public static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    public static boolean isEmpty(Object[] a) {
        return a == null || a.length == 0;
    }

    public static boolean isEmpty(String s) {
        return TextUtils.isEmpty(s);
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Collection c) {
        return c == null || c.isEmpty();
    }

    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Map m) {
        return m == null || m.isEmpty();
    }

    public static boolean isEmpty(JSONArray a) {
        return a == null || a.length() == 0;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean equals(Collection c1, Collection c2) {
        if (isEmpty(c1) && isEmpty(c2)) return true;
        if (isEmpty(c1) || isEmpty(c2)) return false;
        if (c1.size() != c2.size()) return false;
        Set s1 = new HashSet(c1);
        Set s2 = new HashSet(c2);
        return s1.containsAll(s2);
    }

    public static void logLongString(final String tag, final String str) {
        if(str.length() > 4000) {
            Log.d(tag, str.substring(0, 4000));
            logLongString(tag, str.substring(4000));
        } else
            Log.d(tag, str);
    }

    public static void logStackTrace(String tag) {
        Log.d(tag, "====================================================");
        logLongString(tag, TextUtils.join("\n", Thread.currentThread().getStackTrace()));
        Log.d(tag, "====================================================");
    }

    public static String extractEmailDomain(String email) {
        String[] splitted = email.split("@");
        return splitted != null && splitted.length == 2 ? splitted[1] : null;
    }

    public static View getRootView(Activity activity) {
        return activity.getWindow().getDecorView().findViewById(android.R.id.content);
    }

    public static void focusNothing(Activity activity) {
        focusNothing(getRootView(activity));
    }

    public static void focusNothing(View rootView) {
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    public static int[] getDisplaySizes() {
        Context context = PcInternal.getInstance().getCurrentContext();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        if (Build.VERSION.SDK_INT < 13) {
            return new int[] {display.getWidth(), display.getHeight() };
        } else {
            Point point = new Point();
            display.getSize(point);
            return new int[] { point.x, point.y };
        }
    }

    public static Object getArgAt(int index, Object...args) {
        return args != null && args.length > index ? args[index] : null;
    }

    public static String getCountText(int unreadCount) {
        return unreadCount < 100 ? String.valueOf(unreadCount) : "99+";
    }

    public static String md5(final String name) {
        try {
            // create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(name.getBytes());
            byte messageDigest[] = digest.digest();

            // create hex string
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Unable to hash name in md5", e);
        }
        return encodeFileName(name);
    }

    private static String encodeFileName(String name) {
        if (name == null) return "default";
        String s = name;
        s = s.replaceAll("/", "_");
        s = s.replaceAll(":", "_");
        s = s.replaceAll("\\?", "_");
        return s;
    }

    public static String getCacheAsbPath(String relativePath) {
        File cacheDir = PcInternal.getInstance().getCurrentContext().getCacheDir();
        return cacheDir.getAbsolutePath().concat("/").concat(relativePath);
    }

    public static void saveCacheFile(byte[] in, String path) throws IOException {
        saveFile(in, getCacheAsbPath(path));
    }

    public static void saveFile(byte[] in, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(filePath);
        out.write(in);
        out.close();
    }

    public static byte[] readCacheFile(String path) throws IOException {
        return readFile(getCacheAsbPath(path));
    }

    public static byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        FileInputStream in = new FileInputStream(file);
        byte[] b = new byte[in.available()];
        in.read(b);
        in.close();

        return b;
    }

    private static final int TOAST_LENGTH = 1000;
    private static final int TOAST_ANIMATION_LENGTH = 500;
    public static void showToast(final int messageResId) {
        Runnable action = new Runnable() {
            @Override
            public void run() {
                Context context = PcInternal.getInstance().getCurrentContext();
                SuperToast toast = new SuperToast(context);
                toast.setDuration(TOAST_LENGTH - TOAST_ANIMATION_LENGTH);
                toast.setAnimation(SuperToast.ANIMATION_FADE);
                toast.setBackgroundResource(R.drawable.pc_offline_background);
                toast.setText(context.getString(messageResId));
                toast.setTextColor(0xffffffff);
                toast.setTextSize(16);
                toast.setGravity(Gravity.CENTER);
                toast.setXYCoordinates(0, 0);
                toast.show();
            }
        };
        if (isMainThread()) {
            action.run();
        } else {
            PcInternal.getMainThread().post(action);
        }
    }

    public static void showAlert(final int titleResId, final int messageResId, final Runnable postTask) {
        PcInternal.getMainThread().post(new Runnable() { 
            @Override
            public void run() {
                new AlertDialog.Builder(PcInternal.getInstance().getCurrentContext())
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setNegativeButton(R.string.pc_ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (postTask != null) PcInternal.getMainThread().post(postTask);
                    }
                })
                .show();
            }
        });
    }

    private static ProgressDialog sProgressDialog;
    private static int sProgressDialogCount;
    public synchronized static void showProgressDialog() {
        showProgressDialog(R.string.pc_wait);
    }
    public synchronized static void showProgressDialog(final int messageResId) {
        PcInternal.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                Context context = PcInternal.getInstance().getCurrentContext();
                if (sProgressDialogCount == 0) {
                    sProgressDialog = new ProgressDialog(context);
                    sProgressDialog.setMessage(context.getString(messageResId));
                    sProgressDialog.setCancelable(true);
                    sProgressDialog.show();
                }
                sProgressDialogCount++;
            }
        });
    }

    public synchronized static void hideProgressDialog() {
        PcInternal.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                sProgressDialogCount--;
                if (sProgressDialogCount == 0) {
                    if (sProgressDialog != null && sProgressDialog.isShowing()) {
                        sProgressDialog.hide();
                    }
                    sProgressDialog = null;
                }
            }
        });
    }

    public synchronized static void clearAllProgressDialogs() {
        sProgressDialogCount = 0;
        if (sProgressDialog != null && sProgressDialog.isShowing()) {
            sProgressDialog.hide();
        }
        sProgressDialog = null;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void removeOnGlobalLayoutListener(View view, OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < 16) {
            view.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    public static void sendEmail(String subject, String[] emails, Object text, String title) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(EMAIL_TYPE);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_EMAIL, emails);
        if (text instanceof String) {
            intent.putExtra(Intent.EXTRA_TEXT, (String)text);
        } else if (text instanceof Spanned) {
            intent.putExtra(Intent.EXTRA_TEXT, (Spanned)text);
        }
        try {
            PcInternal.getInstance().getCurrentContext()
            .startActivity(Intent.createChooser(intent, title));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Cannot send email", e);
            PcUtils.showAlert(R.string.pc_error, R.string.pc_alert_send_email_failed, null);
        }
    }

    public static void copyAssetFiles(Context context, Pattern pattern) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] fileList = assetManager.list("");
        for (String path : fileList) {
            if (pattern == null || pattern.matcher(path).matches()) {
                copyAssetFile(context, path, path);
            }
        }
    }

    public static void copyAssetFile(Context context, String src, String dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        AssetManager assets = context.getAssets();
        in = assets.open(src);
        out = context.openFileOutput(dst, Context.MODE_PRIVATE);

        copyFile(in, out);
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }

        in.close();
        out.flush();
        out.close();
    }

    public static boolean motionEventOnView(MotionEvent event, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int w = view.getWidth();
        int h = view.getHeight();
        Rect rect = new Rect(x, y, x+w, y+h);
        return rect.contains((int)event.getRawX(), (int)event.getRawY());
    }

    public static Bitmap loadBitmapFromInternalStorage(Context context, String path) {
        if (isEmpty(path)) return null;
        FileInputStream is = null;
        Bitmap bm = null;
        try {
            is = context.openFileInput(path);
            bm = BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + path, e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // ignored
            }
        }
        return bm;
    }

    public static void startHttpLink(String linkString, boolean isWebviewHavingFooter) {
        linkString = lowerCaseHttpxPrefix(linkString);
        Activity activity = (Activity) PcInternal.getInstance().getCurrentContext();
        Intent intent = new Intent(activity, PcWebActivity.class);
        intent.putExtra(PcWebActivity.EXTRA_KEY_HAS_FOOTER, isWebviewHavingFooter);
        intent.putExtra(PcWebActivity.EXTRA_KEY_URL, linkString);
        activity.startActivity(intent);
    }

    private static String lowerCaseHttpxPrefix(String linkString) {
        Pattern pattern = Pattern.compile("(?i:http[s]?)");
        Matcher m = pattern.matcher(linkString);
        while (m.find()) {
            String oldPrefix = m.group(0);
            String newPrefix = (oldPrefix.length()==4)?"http":"https";
            String lowerLinkString = linkString.replaceFirst(oldPrefix,newPrefix);
            return lowerLinkString;
        }
        //Should never come here
        return "";
    }

    public static boolean isAppInstalled(String packageName) {

        if (PcUtils.isEmpty(packageName)) return false;

        PackageManager pm = PcInternal.getInstance().getCurrentContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            // do nothing
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void copyTextToClipboard(String text) {
        if (Build.VERSION.SDK_INT < 11) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) PcInternal.getInstance().getCurrentContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) PcInternal.getInstance().getCurrentContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("PoketChat Copy To Clipboard", text));
        }
    }

    public static void openApp(String packageName) {
        Context context = PcInternal.getInstance().getCurrentContext();
        PackageManager manager = context.getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(intent);
    }

    public static void openDownloadPage(String downloadUrl) {
        Context context = PcInternal.getInstance().getCurrentContext();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(downloadUrl));
        context.startActivity(intent);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void setBackgroundDrawable(View view, Drawable colorDrawable) {
        if (view == null) return;

        if (Build.VERSION.SDK_INT >= 16) {
            view.setBackground(colorDrawable);
        } else {
            view.setBackgroundDrawable(colorDrawable); 
        }
    }

    public static int getMinKeyboardHeight() {
        return pxFromDp(60);
    }

    public static void deleteInternalStorageFile(String path) {
        Context context = PcInternal.getInstance().getCurrentContext();
        context.deleteFile(path);
    }

    public static String fillZero(String numberString, int minLength) {
        if (numberString == null) numberString = "";

        int diff = minLength - numberString.length();
        for (int i = 0; i < diff; i++) {
            numberString = "0" + numberString;
        }

        return numberString;
    }

    public static int getImageRotateAngle(String imagePath) throws IOException {
        ExifInterface exif = new ExifInterface(imagePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int angle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            angle = 90;
        } 
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            angle = 180;
        } 
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            angle = 270;
        }

        return angle;
    }

    public static Bitmap correctBitmapOrientation(String path, Bitmap bm) {
        if (path != null && bm != null) {
            int angle = 0;
            try {
                angle = getImageRotateAngle(path);
                if (angle != 0) {

                    Matrix matrix = new Matrix();
                    matrix.postRotate(angle);
                    Bitmap rotatedBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);

                    bm.recycle();
                    bm = rotatedBm;
                }
            } catch (IOException e) {
                Log.e(TAG, "Can not rotate bitmap path: " + path + ", angle:" + angle, e);
            }
        }
        return bm;
    }
}