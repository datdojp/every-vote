package jp.co.mobilus.mobilib.util;

import android.os.Environment;

public class PcConstants {

    // urls
    public static final String CHAT_SERVICE_URL = "wss://pkchat.mbcld.net/chat";
    public static final String POKELABO_API_BASE_URL = "http://chat.api.pokelabo.jp/v1/";
    public static final String TERMS_OF_SERVICE_URL = POKELABO_API_BASE_URL + "rule";
    public static final String PRIVACY_POLICY_URL = POKELABO_API_BASE_URL + "policy";
    public static final String MAINTENANCE_URL = "http://pokelabo.co.jp";

    // switch on/off features
    public static final boolean STAMP_SHOP_ENABLED = true;
    public static final boolean USE_GOOGLE_LOGIN = false;
    public static final boolean VALIDATE_SSL_CERTIFICATE_FOR_CHAT = false;
    public static final boolean VALIDATE_SSL_CERTIFICATE_FOR_API = false;
    public static final boolean SKIP_TUTORIAL_CHECK = false;

    // keys & ids
    public static final String GCM_SENDER_ID = "795297978867";
    public static final String GOOGLE_CLIENT_ID = "974596691011.apps.googleusercontent.com";
    public static final String GOOGLE_CLIENT_SECRET = "4RRiWGUY_mngt_IW8STBi2Fl";
    public static final String DEFAULT_GAME_ID = "guardianbattle";
    public static final String BOT_ROOM_POKE_ID = "10000000000000000001";

    // image upload/download
    public static final int IMAGE_MESSAGE_FULL_SIZE_MAX_SIZE = 1280;
    public static final int IMAGE_MESSAGE_THUMBNAIL_MAX_SIZE = 160;
    public static final int ROOM_ICON_WIDTH_IN_PX = 300;
    public static final int ROOM_ICON_HEIGHT_IN_PX = 375;
    public static final String MESSAGE_IMAGE_STORING_FOLDER =
            Environment.getExternalStorageDirectory() + "/" +
                    Environment.DIRECTORY_DCIM + "/" +
                    "PokeChat" + "/";
    public static final String TAKEN_IMAGE_STORING_FOLDER = MESSAGE_IMAGE_STORING_FOLDER;
    public static final String[] IMAGE_PICKING_FOLDERS = new String[] {
        Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM,
        Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES
    };
    public static final String[] IMAGE_PICKING_EXTENSIONS = new String[] {
        "jpg", "jpeg", "png", "gif"
    };
}
