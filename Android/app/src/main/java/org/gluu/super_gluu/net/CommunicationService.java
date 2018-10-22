/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.gluu.super_gluu.app.GluuApplication;
import org.gluu.super_gluu.util.CertUtils;
import org.gluu.super_gluu.util.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import SuperGluu.app.BuildConfig;

/**
 * Network communication service
 *
 * Created by Yuriy Movchan on 12/28/2015.
 */
public class CommunicationService {

    private static final String TAG = "communication-service";

    public static String get(String baseUrl, Map<String, String> params) throws IOException {
        if (BuildConfig.DEBUG) Log.d(TAG, "Attempting to execute get with parameters: " + params);

        HttpURLConnection connection = null;
        try {
            String urlParameters = getEncodedUrlParameters(params);
            URL url;
            if (urlParameters == null) {
                url = new URL(baseUrl);
            } else {
                url = new URL(baseUrl + '?' + urlParameters);
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");

            //Get Response
            Log.v(TAG,"Response code is: " + connection.getResponseCode());
            InputStream is = connection.getInputStream();

            return readStream(is);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String post(String baseUrl, Map<String, String> params) throws IOException {
        if (BuildConfig.DEBUG) Log.d(TAG, "Attempting to execute post with parameters: " + params);

        HttpURLConnection connection = null;
        try {
            String urlParameters = getEncodedUrlParameters(params);

            URL url = new URL(baseUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            // Read Response
            InputStream is = connection.getInputStream();
            return readStream(is);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String getEncodedUrlParameters(Map<String, String> params) throws UnsupportedEncodingException {
        if (params == null) {
            return null;
        }

        StringBuilder urlParametersBuilder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();

            String key = param.getKey();
            String value = param.getValue();

            if (BuildConfig.DEBUG) Log.d(TAG, key + " = " + value);
            if (value == null) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Key '" + key + "' value is null");
                continue;
            }

            urlParametersBuilder.append(key).append('=').append(URLEncoder.encode(value, "UTF-8"));
            if (iterator.hasNext()) {
                urlParametersBuilder.append('&');
            }
        }

        return urlParametersBuilder.toString();
    }

    private static String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line;
        StringBuilder result = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();

        return result.toString();
    }

    /**
     * Checking for all possible internet providers
     */
    public boolean isConnectingToInternet(Context context){
        ConnectivityManager connectivity =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] networkInfos = connectivity.getAllNetworkInfo();
            if (networkInfos == null) {
                return false;
            }

            for (NetworkInfo networkInfo : networkInfos) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }
    public static void initTrustAllTrustManager() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {


                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        // not implemented
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {
                        // not implemented
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                }
        };

        try {

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }

            });
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void initTrustCertTrustManager(String certificate, boolean skipHostnameVerification) {
        // Load certificate
        X509Certificate cert = CertUtils.loadCertificate(certificate);

        if (cert == null) {
            Log.e(TAG, "Failed to load certificate");
        } else {
            initTrustCertTrustManager(cert, skipHostnameVerification);
        }
    }

    public static void initTrustCertTrustManager(X509Certificate cert, boolean skipHostnameVerification) {
            try {
            String alias = cert.getSubjectX500Principal().getName();

            // Create trust store
            KeyStore trustStore = null;
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);
            trustStore.setCertificateEntry(alias, cert);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(trustStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();

            // Install the trust-cert trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to install Trust Cert TrustManager", ex);
        }

        if (skipHostnameVerification) {
            setTrustAllHostnameVerifier();
        }
    }

    private static void setTrustAllHostnameVerifier() {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    public static void init() {

        Log.v(TAG,"init() called and isTrustAllCertificates value is:"+GluuApplication.isTrustAllCertificates);

        if (GluuApplication.isTrustAllCertificates) {
            initTrustAllTrustManager();
            return;
        }

        // Init trust manager to trust only specific server and skip hostname verification
        if (Utils.isNotEmpty(BuildConfig.OX_SERVER_CERT)) {
            initTrustCertTrustManager(BuildConfig.OX_SERVER_CERT, false);
        }

//        if (BuildConfig.DEBUG) {
//
//            Log.v(TAG,"Debug");
//
//            // Init trust all manager
//            if (BuildConfig.TRUST_ALL_CERT) {
//                initTrustAllTrustManager();
//                return;
//            }
//
//            // Init trust manager to trust only specific server and skip hostname verifiaction
//            if (Utils.isNotEmpty(BuildConfig.OX_SERVER_CERT)) {
//                initTrustCertTrustManager(BuildConfig.OX_SERVER_CERT, true);
//            }
//        } else {
//            Log.v(TAG,"Release");
//            // Init trust manager to trust only specific server
//            if (Utils.isNotEmpty(BuildConfig.OX_SERVER_CERT)) {
//                initTrustCertTrustManager(BuildConfig.OX_SERVER_CERT, false);
//            }
//        }
    }

    public static void validateCert(){
        TrustManagerFactory tmf;
        try {

//            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            keyStore.load(new File(Environment.getExternalStorageDirectory())., "Pwd");
//            trustStore.close();

//            URL url = new URL(strURL);
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            tmf.init(keyStore);
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, tmf.getTrustManagers(), null);
            SSLSocketFactory sslFactory = ctx.getSocketFactory();
//            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
//            conn.setSSLSocketFactory(sslFactory);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslFactory);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.v("TAG","No Such Algorithm exception in validate cert "+e.getMessage());
        } catch (KeyManagementException e) {
            e.printStackTrace();
            Log.v("TAG","KeyManagementException in validate cert "+e.getMessage());
        }
    }
}
