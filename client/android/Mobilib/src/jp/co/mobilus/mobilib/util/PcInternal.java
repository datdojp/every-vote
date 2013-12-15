package jp.co.mobilus.mobilib.util;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.pokelabo.pokechat.db.DBUser;
import jp.co.pokelabo.pokechat.db.DBUserGameInfo;
import jp.co.pokelabo.pokechat.model.PcGame;
import jp.co.pokelabo.pokechat.model.PcUser;
import junit.framework.Assert;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class PcInternal {
    private static final String PREF_KEY_TERM_OF_SERVICE_ACCEPTED = "term_of_service_accepted";

    private static final String PREF_KEY_LAST_LOGGED_IN_CHAT = "last_logged_in_chat";
    private static final String PREF_KEY_PREVIOUS_LOGGED_IN_CHAT = "previous_logged_in_chat";
    private static final String PREF_KEY_LAST_LOGGED_OUT_CHAT = "last_logged_out_chat";

    private static Handler sMainThread = new Handler(Looper.getMainLooper());
    private static Map<String, Object> sCommonBundle = new ConcurrentHashMap<String, Object>();

    private static SharedPreferences sPrefs;
    private Context mCurrentContext;
    private PcUser mCurrentUser;
    private String mCurrentGameId = null;
    private PcGame mCurrentGame;

    private static PcInternal mInstance;
    public static PcInternal getInstance() {
        return mInstance;
    }

    private PcInternal(Context context) {
        mCurrentContext = context;
    }

    public static void createInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PcInternal(context);
        } else {
            mInstance.mCurrentContext = context;
        }
    }

    public void saveCurrentUserInfo(String email, String name, String avatar) {
        mCurrentUser = new PcUser();
        mCurrentUser.setId(email);
        mCurrentUser.getUserGameInfo().setNickName(name);
        mCurrentUser.getUserGameInfo().setThumbnailUrl(avatar);
        mCurrentUser.setMe(true);
        DBUser.insertOrUpdate(mCurrentUser);
        DBUserGameInfo.insertOrUpdate(mCurrentUser.getUserGameInfo());
    }

    public static Handler getMainThread() {
        return sMainThread;
    }

    public void loadCurrentUser() {
        mCurrentUser = PcUser.getMe();
    }

    public SharedPreferences getPrefs() {
        return getPrefs(getCurrentContext());
    }

    public static SharedPreferences getPrefs(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return sPrefs;
    }

    public PcUser getCurrentUser() {
        return mCurrentUser;
    }

    public Context getCurrentContext() {
        return mCurrentContext;
    }

    public void setCurrentContext(Context context) {
        mCurrentContext = context;
    }

    public PcGame getCurrentGame() {
        if (mCurrentGame == null) {
            mCurrentGame = PcGame.getOne(getGameId());
        }
        if (mCurrentGame == null) { // to prevent NullPointerException
            mCurrentGame = new PcGame();
            mCurrentGame.setGid(getGameId());
        }
        return mCurrentGame;
    }

    public Locale getLocale() {
        if (mCurrentContext != null) {
            return mCurrentContext.getResources().getConfiguration().locale;
        } else {
            return Locale.JAPAN;
        }
    }

    public int getAvatarWidth() {
        return -1;
    }

    public int getAvatarHeight() {
        return -1;
    }

    public int getRoomImageWidth() {
        return -1;
    }

    public int getRoomImageHeight() {
        return -1;
    }

    public boolean didAcceptTermOfService() {
        return getPrefs().getBoolean(PREF_KEY_TERM_OF_SERVICE_ACCEPTED, false);
    }

    public void markTermOfServiceAccepted() {
        getPrefs().edit().putBoolean(PREF_KEY_TERM_OF_SERVICE_ACCEPTED, true).commit();
    }

    public String getGameId() {
        if(mCurrentGameId == null) {
            return PcConstants.DEFAULT_GAME_ID; // TODO: why always jump to this line?
        } else{
            return mCurrentGameId;
        }
    }

    public void setGameId(String newGameId) {
        if (!TextUtils.equals(getGameId(), newGameId)) {
            mCurrentGameId = newGameId;

            // reset game
            if (mCurrentGame != null) {
                mCurrentGame = null;
                getCurrentGame();
            }
        }
    }

    public static void executeOnAsyncThread(final Runnable action) {
        Assert.assertNotNull(action);
        executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                new PcAsyncTask() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        action.run();
                        return null;
                    }
                }.execute();
            }
        });
    }

    public static void executeOnMainThread(Runnable action) {
        Assert.assertNotNull(action);
        Context context = PcInternal.getInstance().getCurrentContext();
        if (context instanceof Activity) {
            ((Activity)context).runOnUiThread(action);
        } else {
            if (PcUtils.isMainThread()) {
                action.run();
            } else {
                sMainThread.post(action);
            }
        }
    }

    public static void putToCommonBundle(String key, Object value) {
        sCommonBundle.put(key, value);
    }

    public static String putToCommonBundle(Object value) {
        String key = UUID.randomUUID().toString();
        sCommonBundle.put(key, value);
        return key;
    }

    public static Object getFromCommonBundle(String key) {
        return sCommonBundle.get(key);
    }

    public static Object removeFromCommonBundle(String key) {
        return sCommonBundle.remove(key);
    }

    public void saveLastLoggedInChat() {
        long lastLoggedInChat = getPrefs().getLong(PREF_KEY_LAST_LOGGED_IN_CHAT, 0);
        getPrefs()
        .edit()
        .putLong(PREF_KEY_PREVIOUS_LOGGED_IN_CHAT, lastLoggedInChat)
        .putLong(PREF_KEY_LAST_LOGGED_IN_CHAT, System.currentTimeMillis())
        .commit();
    }

    public long getLatestLogInOutBeforeLastLogIn() {
        return Math.max(
                getPrefs().getLong(PREF_KEY_LAST_LOGGED_OUT_CHAT, 0),
                getPrefs().getLong(PREF_KEY_PREVIOUS_LOGGED_IN_CHAT, 0));
    }

    public void saveLastLoggedOutChat() {
        getPrefs()
        .edit()
        .putLong(PREF_KEY_LAST_LOGGED_OUT_CHAT, System.currentTimeMillis())
        .commit();
    }
}
