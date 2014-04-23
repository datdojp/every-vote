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
import java.util.regex.Pattern;

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
import android.content.pm.PackageInfo;
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
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class MblUtils extends MblInternal {
    private static final String TAG = getTag(MblUtils.class);
    private static float density = 0;
    private static final String EMAIL_TYPE = "message/rfc822";

    public static void showKeyboard(View focusedView) {
        focusedView.requestFocus();
        InputMethodManager inputMethodManager = ((InputMethodManager)getCurrentContext().getSystemService(Context.INPUT_METHOD_SERVICE));
        inputMethodManager.showSoftInput(focusedView, InputMethodManager.SHOW_FORCED);
    }
    
    public static void hideKeyboard() {
        Activity activity = (Activity) getCurrentContext();
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
            density = getCurrentContext().getResources().getDisplayMetrics().density;
        }
        return (int) (dp * density);
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static boolean isPortraitDisplay() {
        return getCurrentContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static boolean isPortraitWindow() {
        Activity activity = (Activity) getCurrentContext();
        View root = activity.getWindow().getDecorView();
        return root.getWidth() <= root.getHeight();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static boolean isNetworkConnected() {
        return isNetworkConnected(getCurrentContext());
    }

    public static Bitmap loadBitmapMatchSpecifiedSize(final int targetW, final int targetH, final byte[] bmData) {
        return new LoadBitmapMatchSpecifiedSizeTemplate<byte[]>() {

            @Override
            public int[] getBitmapSizes(byte[] bmData) {
                return MblUtils.getBitmapSizes(bmData);
            }

            @Override
            public Bitmap decodeBitmap(byte[] bmData, BitmapFactory.Options options) {
                return BitmapFactory.decodeByteArray(bmData, 0, bmData.length, options);
            }

        }.load(targetW, targetH, bmData);
    }

    public static Bitmap loadBitmapMatchSpecifiedSize(final int targetW, final int targetH, final String path) {
        return new LoadBitmapMatchSpecifiedSizeTemplate<String>() {

            @Override
            public int[] getBitmapSizes(String path) {
                try {
                    return MblUtils.getBitmapSizes(path);
                } catch (IOException e) {
                    return new int[] {0, 0};
                }
            }

            @Override
            public Bitmap decodeBitmap(String path, BitmapFactory.Options options) {
                return BitmapFactory.decodeFile(path, options);
            }

        }.load(targetW, targetH, path);
    }

    private static abstract class LoadBitmapMatchSpecifiedSizeTemplate<T> {

        public abstract int[] getBitmapSizes(T input);
        public abstract Bitmap decodeBitmap(T input, BitmapFactory.Options options);

        public Bitmap load(final int targetW, final int targetH, T input) {
            int scaleFactor = 1;

            // get image view sizes
            int photoW = 0;
            int photoH = 0;
            if (targetW > 0 || targetH > 0) {
                // get bitmap sizes
                int[] photoSizes = getBitmapSizes(input);
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
            Bitmap bm = decodeBitmap(input, bmOptions);
            return bm;
        }
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
        BitmapFactory.decodeResource(getCurrentContext().getResources(), resId, bmOptions);
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
        Context context = getCurrentContext();
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
        File cacheDir = getCurrentContext().getCacheDir();
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
    
    public static byte[] readInternalFile(String filePath) throws IOException {
        FileInputStream in = getCurrentContext().openFileInput(filePath);
        byte[] b = new byte[in.available()];
        in.read(b);
        in.close();

        return b;
    }

    public static void showAlert(final int titleResId, final int messageResId, final Runnable postTask) {
        executeOnMainThread(new Runnable() { 
            @Override
            public void run() {
                new AlertDialog.Builder(getCurrentContext())
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setNegativeButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (postTask != null) getMainThreadHandler().post(postTask);
                    }
                })
                .show();
            }
        });
    }

    private static ProgressDialog sProgressDialog;
    private static int sProgressDialogCount;
    public synchronized static void showProgressDialog(final int messageResId) {
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                Context context = getCurrentContext();
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
        executeOnMainThread(new Runnable() {
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

    public static void showToast(final String text, final int duration) {
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getCurrentContext(), text, duration).show();
            }
        });
    }

    public static void showToast(final int resId, final int duration) {
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        getCurrentContext(),
                        getCurrentContext().getString(resId),
                        duration).show();
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

    public static boolean sendEmail(String subject, String[] emails, Object text, String title) {
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
            getCurrentContext()
            .startActivity(Intent.createChooser(intent, title));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Cannot send email", e);
            return false;
        }
        return true;
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

    public static boolean isAppInstalled(String packageName) {

        if (MblUtils.isEmpty(packageName)) return false;

        PackageManager pm = getCurrentContext().getPackageManager();
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
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getCurrentContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getCurrentContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("PoketChat Copy To Clipboard", text));
        }
    }

    public static void openApp(String packageName) {
        Context context = getCurrentContext();
        PackageManager manager = context.getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(intent);
    }

    public static void openDownloadPage(String downloadUrl) {
        Context context = getCurrentContext();
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
        Context context = getCurrentContext();
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

    public static void scrollListViewToBottom(final ListView listView) {
        if (listView == null || listView.getAdapter() == null) {
            return;
        }

        final Runnable action = new Runnable() {
            @Override
            public void run() {
                int count = listView.getAdapter().getCount();
                if (count > 0) {
                    listView.setSelectionFromTop(count, -listView.getHeight());
                }
            }
        };

        if (listView.getHeight() > 0) {
            executeOnMainThread(action);
        } else {
            listView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    removeOnGlobalLayoutListener(listView, this);
                    action.run();
                }
            });
        }
    }

    @SuppressLint("NewApi")
    public static String generateDeviceId() {
        Context context = MblUtils.getCurrentContext();
        StringBuilder builder = new StringBuilder();

        // android id
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID); 
        if (!TextUtils.isEmpty(androidId)) builder.append(androidId);

        // serial
        if (Build.VERSION.SDK_INT >= 9) {
            String serial = Build.SERIAL;
            if (!TextUtils.isEmpty(serial) && !Build.UNKNOWN.equals(serial)) builder.append(serial);
        }

        // phone device id
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        if (!TextUtils.isEmpty(deviceId)) builder.append(deviceId);

        // combine & hast
        return md5(builder.toString());
    }

    public static void loadInternalImage(
            final String path,
            final ImageView target) {

        if (TextUtils.isEmpty(path)) return;

        executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bm = loadInternalImage(path);
                executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        target.setImageBitmap(bm);
                    }
                });
            }
        });
    }

    public static Bitmap loadInternalImage(String path) {
        if (TextUtils.isEmpty(path)) return null;

        FileInputStream is = null;
        Bitmap bm = null;
        try {
            is = getCurrentContext().openFileInput(path);
            bm = BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Can not load image from internal storage: path=" + path, e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                // ignored
            }
        }
        return bm;
    }

    public static void copyAssetFileToExternalMemory(String src, String dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        AssetManager assets = getCurrentContext().getAssets();
        in = assets.open(src);
        out = new FileOutputStream(dst);

        copyFile(in, out);
    }

    // ref: http://stackoverflow.com/questions/3105673/android-how-to-kill-an-application-with-all-its-activities
    public static void closeApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static PackageInfo getAppPackageInfo() {
        try {
            Context context = MblUtils.getCurrentContext();
            String packageName = context.getPackageName();
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            Log.i(TAG, "Could not get app name and version", e);
        }
        return null;
    }
}
