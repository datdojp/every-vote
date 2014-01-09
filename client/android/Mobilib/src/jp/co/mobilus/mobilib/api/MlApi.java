package jp.co.mobilus.mobilib.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.mobilus.mobilib.util.MlInternal;
import jp.co.mobilus.mobilib.util.MlUtils;
import jp.co.mobilus.mobilib.util.MlSSLCertificateUtils;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public abstract class MlApi {

    private static final String TAG = MlApi.class.getSimpleName();

    private final Map<String, Vector<MlApiGetCallback>> mGetRequestCallbacks = new ConcurrentHashMap<String, Vector<MlApiGetCallback>>();

    public void get(
            final String url,
            final Map<String, String> params,
            final Map<String, String> headerParams,
            final boolean isCacheEnabled,
            final boolean isBinaryRequest,
            final boolean isIgnoreSSLCertificate,
            final SQLiteDatabase db, 
            MlApiGetCallback callback ) {

        final String fullUrl = generateGetMethodFullUrl(url, params);

        synchronized (mGetRequestCallbacks) {

            Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
            if (allCallbacksForFullUrl == null) {
                allCallbacksForFullUrl = new Vector<MlApiGetCallback>();
                mGetRequestCallbacks.put(fullUrl, allCallbacksForFullUrl);
            }
            synchronized (allCallbacksForFullUrl) {
                boolean isNoCallback = allCallbacksForFullUrl.isEmpty();
                if (callback != null) allCallbacksForFullUrl.add(callback);
                if (!isNoCallback) { // another request for fullUrl is already sent and waiting for response
                    return;
                }
            }
        }

        MlInternal.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                MlCache existingCache = null;
                if (isCacheEnabled) {
                    existingCache = MlCache.get(db, fullUrl);
                    boolean shouldReadFromCache =
                            existingCache != null &&
                            (   !MlUtils.isNetworkConnected() ||
                                    System.currentTimeMillis() - existingCache.getDate() <= getCacheDuration(fullUrl, isBinaryRequest)    );
                    if (shouldReadFromCache) {
                        try {
                            byte[] data = MlUtils.readCacheFile(existingCache.getFileName());
                            if (data != null) {
                                Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                                synchronized (allCallbacksForFullUrl) {
                                    for (MlApiGetCallback cb : allCallbacksForFullUrl) {
                                        if (isBinaryRequest) cb.onSuccess(data);
                                        else cb.onSuccess(new String(data));
                                    }
                                    allCallbacksForFullUrl.clear();
                                }

                                return;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Cache not exist", e);
                        }
                    }
                }

                try {

                    HttpClient httpClient = getHttpClient(fullUrl, isIgnoreSSLCertificate);
                    HttpContext httpContext = new BasicHttpContext();
                    HttpGet httpGet = new HttpGet(fullUrl);

                    httpGet.setHeaders(getHeaderArray(headerParams));

                    HttpResponse response = httpClient.execute(httpGet, httpContext);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode > 299) {
                        Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                        synchronized (allCallbacksForFullUrl) {
                            for (MlApiGetCallback cb : allCallbacksForFullUrl) {
                                cb.onFailure(statusCode, response.getStatusLine().getReasonPhrase());
                            }
                            allCallbacksForFullUrl.clear();
                        }

                        return;
                    }

                    byte[] data = EntityUtils.toByteArray(response.getEntity());

                    if (isCacheEnabled) {
                        saveCache(db, existingCache, fullUrl, data);
                    }

                    Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                    synchronized (allCallbacksForFullUrl) {
                        for (MlApiGetCallback cb : allCallbacksForFullUrl) {
                            if (isBinaryRequest) cb.onSuccess(data);
                            else cb.onSuccess(new String(data));
                        }
                        allCallbacksForFullUrl.clear();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download file", e);

                    Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                    synchronized (allCallbacksForFullUrl) {
                        for (MlApiGetCallback cb : allCallbacksForFullUrl) {
                            cb.onFailure(-1, "Unknown error");
                        }
                        allCallbacksForFullUrl.clear();
                    }
                }
            }
        });
    }

    public static abstract class MlApiGetCallback {
        public void onSuccess(byte[] data) {}
        public void onSuccess(String data) {}
        public abstract void onFailure(int error, String errorMessage);
    }

    public void post(
            final String url,
            final Map<String, String> params,
            final Map<String, String> headerParams,
            final boolean isIgnoreSSLCertificate,
            final MlApiPostCallback callback ) {

        MlInternal.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                try {

                    HttpClient httpClient = getHttpClient(url, isIgnoreSSLCertificate);
                    HttpContext httpContext = new BasicHttpContext();
                    HttpPost httpPost = new HttpPost(url);

                    if (!MlUtils.isEmpty(params)) {
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        for (String key : params.keySet()) {
                            nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
                        }
                        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    }

                    httpPost.setHeaders(getHeaderArray(headerParams));

                    HttpResponse response = httpClient.execute(httpPost, httpContext);

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode > 299) {
                        if (callback != null) {
                            callback.onFailure(
                                    statusCode,
                                    response.getStatusLine().getReasonPhrase());
                        }
                        return;
                    }

                    String data = EntityUtils.toString(response.getEntity());

                    if (callback != null) callback.onSuccess(statusCode, data);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to download file", e);
                    if (callback != null) callback.onFailure(-1, "Unknown error");
                }
            }
        });
    }

    public static abstract class MlApiPostCallback {
        public void onSuccess(int statusCode, String data) {}
        public abstract void onFailure(int error, String errorMessage);
    }

    private static final long _5_MIN = 1000l * 60l * 5l;
    private static final long _1_HOUR = 1000l * 60l * 60l;
    protected long getCacheDuration(String url, boolean isBinaryRequest) {
        if (isBinaryRequest) {
            return _1_HOUR;
        } else {
            return _5_MIN;
        }
    }

    private HttpClient getHttpClient(String url, boolean ignoreSSLCertificate) {
        if (MlSSLCertificateUtils.isHttpsUrl(url) && ignoreSSLCertificate) {
            return MlSSLCertificateUtils.getHttpClientIgnoreSSLCertificate();
        } else {
            return new DefaultHttpClient();
        }
    }

    private void saveCache(SQLiteDatabase db, MlCache existingCache, String fullUrl, byte[] data) {
        try {
            MlCache cacheToSave;
            if (existingCache == null) {
                cacheToSave = new MlCache();
                cacheToSave.setKey(fullUrl);
                cacheToSave.setDate(System.currentTimeMillis());
                MlCache.insert(db, cacheToSave);
            } else {
                cacheToSave = existingCache;
                cacheToSave.setDate(System.currentTimeMillis());
                MlCache.update(db, cacheToSave);
            }
            MlUtils.saveCacheFile(data, cacheToSave.getFileName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache url: " + fullUrl, e);
        }
    }

    private String generateGetMethodFullUrl(String url, Map<String, String> params) {
        if (!MlUtils.isEmpty(params)) {
            Uri.Builder builder = Uri.parse(url).buildUpon();
            for (String key : params.keySet()) {
                builder.appendQueryParameter(key, params.get(key));
            }
            return builder.build().toString();
        } else {
            return url;
        }
    }

    private Header[] getHeaderArray(Map<String, String> headerParams) {

        Header[] headers = null;

        if (!MlUtils.isEmpty(headerParams)) {
            headers = new Header[headerParams.keySet().size()];
            int i = 0;
            for (final String key : headerParams.keySet()) {
                final String val = headerParams.get(key);
                headers[i++] = new Header() {

                    @Override
                    public HeaderElement[] getElements() throws ParseException { return null; }

                    @Override
                    public String getName() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return val;
                    }
                };
            }
        }

        return headers;
    }
}