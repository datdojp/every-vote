package jp.co.mobilus.mobilib.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.mobilus.mobilib.util.MblUtils;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.net.Uri;
import android.util.Log;

@SuppressWarnings("deprecation")
public abstract class MblApi {

    private static final String TAG = MblApi.class.getSimpleName();

    private static final String UTF8 = "UTF-8";
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    
    
    private final Map<String, Vector<MlApiGetCallback>> mGetRequestCallbacks = new ConcurrentHashMap<String, Vector<MlApiGetCallback>>();

    public void get(
            final String url,
            final Map<String, String> params,
            final Map<String, String> headerParams,
            final boolean isCacheEnabled,
            final boolean isBinaryRequest,
            final boolean isIgnoreSSLCertificate,
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

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                MblCache existingCache = null;
                if (isCacheEnabled) {
                    existingCache = MblCache.get(fullUrl);
                    boolean shouldReadFromCache =
                            existingCache != null &&
                            (   !MblUtils.isNetworkConnected() ||
                                    System.currentTimeMillis() - existingCache.getDate() <= getCacheDuration(fullUrl, isBinaryRequest)    );
                    if (shouldReadFromCache) {
                        try {
                            final byte[] data = MblUtils.readCacheFile(existingCache.getFileName());
                            if (data != null) {
                                Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                                synchronized (allCallbacksForFullUrl) {
                                    for (final MlApiGetCallback cb : allCallbacksForFullUrl) {
                                        if (isBinaryRequest) {
                                            MblUtils.executeOnMainThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    cb.onSuccess(data);
                                                }
                                            });
                                        } else {
                                            MblUtils.executeOnMainThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    cb.onSuccess(new String(data));
                                                }
                                            });
                                        }
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

                    final HttpResponse response = httpClient.execute(httpGet, httpContext);

                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode > 299) {
                        Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                        synchronized (allCallbacksForFullUrl) {
                            for (final MlApiGetCallback cb : allCallbacksForFullUrl) {
                                MblUtils.executeOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        cb.onFailure(statusCode, response.getStatusLine().getReasonPhrase());
                                    }
                                });
                            }
                            allCallbacksForFullUrl.clear();
                        }

                        return;
                    }

                    final byte[] data = EntityUtils.toByteArray(response.getEntity());

                    if (isCacheEnabled) {
                        saveCache(existingCache, fullUrl, data);
                    }

                    Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                    synchronized (allCallbacksForFullUrl) {
                        for (final MlApiGetCallback cb : allCallbacksForFullUrl) {
                            if (isBinaryRequest) {
                                MblUtils.executeOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        cb.onSuccess(data);
                                    }
                                });
                            } else {
                                MblUtils.executeOnMainThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        cb.onSuccess(new String(data));
                                    }
                                });
                            }
                        }
                        allCallbacksForFullUrl.clear();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download file", e);

                    Vector<MlApiGetCallback> allCallbacksForFullUrl = mGetRequestCallbacks.get(fullUrl);
                    synchronized (allCallbacksForFullUrl) {
                        for (final MlApiGetCallback cb : allCallbacksForFullUrl) {
                            MblUtils.executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    cb.onFailure(-1, "Unknown error");
                                }
                            });
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
            final Map<String, ? extends Object> params,
            final Map<String, String> headerParams,
            final boolean isIgnoreSSLCertificate,
            final MlApiPostCallback callback ) throws MblApiInvalidParam {

        boolean isMultipart = false;
        if (!MblUtils.isEmpty(params)) {
            for (String key : params.keySet()) {
                Object val = params.get(key);
                if (val instanceof InputStream) {
                    isMultipart = true;
                }
                if (!(val instanceof InputStream) && !(val instanceof String)) {
                    throw new MblApiInvalidParam("params must be String or InputStream");
                }
            }
        }
        final boolean fIsMultipart = isMultipart;

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                try {

                    HttpClient httpClient = getHttpClient(url, isIgnoreSSLCertificate);
                    HttpContext httpContext = new BasicHttpContext();
                    HttpPost httpPost = new HttpPost(url);

                    if (!MblUtils.isEmpty(params)) {
                        if (fIsMultipart) {
                            MultipartEntity multipartContent = new MultipartEntity();
                            for (String key : params.keySet()) {
                                Object val = params.get(key);
                                if (val instanceof InputStream) {
                                    multipartContent.addPart(key, new InputStreamBody((InputStream)val, key));
                                } else if (val instanceof String){
                                    multipartContent.addPart(key, new StringBody((String)val, CHARSET_UTF8));
                                }
                            }
                            httpPost.setEntity(multipartContent);
                        } else {
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                            for (String key : params.keySet()) {
                                nameValuePairs.add(new BasicNameValuePair(key, (String)params.get(key)));
                            }
                            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, UTF8));
                        }
                    }

                    httpPost.setHeaders(getHeaderArray(headerParams));

                    final HttpResponse response = httpClient.execute(httpPost, httpContext);

                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode > 299) {
                        if (callback != null) {
                            MblUtils.executeOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(
                                            statusCode,
                                            response.getStatusLine().getReasonPhrase());
                                }
                            });
                        }
                        return;
                    }

                    final String data = EntityUtils.toString(response.getEntity());

                    if (callback != null) {
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(statusCode, data);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Failed to download file", e);
                    if (callback != null) {
                        MblUtils.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(-1, "Unknown error");
                            }
                        });
                    }
                }
            }
        });
    }

    public static abstract class MlApiPostCallback {
        public void onSuccess(int statusCode, String data) {}
        public abstract void onFailure(int error, String errorMessage);
    }

    protected abstract long getCacheDuration(String url, boolean isBinaryRequest);

    private HttpClient getHttpClient(String url, boolean ignoreSSLCertificate) {
        if (MblSSLCertificateUtils.isHttpsUrl(url) && ignoreSSLCertificate) {
            return MblSSLCertificateUtils.getHttpClientIgnoreSSLCertificate();
        } else {
            return new DefaultHttpClient();
        }
    }

    private void saveCache(MblCache existingCache, String fullUrl, byte[] data) {
        try {
            MblCache cacheToSave;
            if (existingCache == null) {
                cacheToSave = new MblCache();
                cacheToSave.setKey(fullUrl);
                cacheToSave.setDate(System.currentTimeMillis());
                MblCache.insert(cacheToSave);
            } else {
                cacheToSave = existingCache;
                cacheToSave.setDate(System.currentTimeMillis());
                MblCache.update(cacheToSave);
            }
            MblUtils.saveCacheFile(data, cacheToSave.getFileName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache url: " + fullUrl, e);
        }
    }

    private String generateGetMethodFullUrl(String url, Map<String, String> params) {
        if (!MblUtils.isEmpty(params)) {
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

        if (!MblUtils.isEmpty(headerParams)) {
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

    @SuppressWarnings("serial")
    public static abstract class MblApiException extends Exception {
        public MblApiException(String msg) {
            super(msg);
        }
    }

    @SuppressWarnings("serial")
    public static class MblApiInvalidParam extends MblApiException {
        public MblApiInvalidParam(String msg) {
            super(msg);
        }
    }
}