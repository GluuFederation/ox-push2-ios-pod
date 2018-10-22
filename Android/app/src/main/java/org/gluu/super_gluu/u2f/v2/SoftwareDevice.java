/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.u2f.v2;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import org.gluu.super_gluu.app.services.AppFirebaseInstanceIDService;
import org.gluu.super_gluu.device.DeviceUuidManager;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.store.AndroidKeyDataStore;
import org.gluu.super_gluu.u2f.v2.cert.KeyPairGeneratorImpl;
import org.gluu.super_gluu.u2f.v2.codec.RawMessageCodec;
import org.gluu.super_gluu.u2f.v2.codec.RawMessageCodecImpl;
import org.gluu.super_gluu.u2f.v2.device.U2FKeyImpl;
import org.gluu.super_gluu.u2f.v2.exception.U2FException;
import org.gluu.super_gluu.u2f.v2.model.AuthenticateRequest;
import org.gluu.super_gluu.u2f.v2.model.AuthenticateResponse;
import org.gluu.super_gluu.u2f.v2.model.DeviceData;
import org.gluu.super_gluu.u2f.v2.model.EnrollmentRequest;
import org.gluu.super_gluu.u2f.v2.model.EnrollmentResponse;
import org.gluu.super_gluu.u2f.v2.model.TokenResponse;
import org.gluu.super_gluu.u2f.v2.store.DataStore;
import org.gluu.super_gluu.u2f.v2.user.UserPresenceVerifierImpl;
import org.gluu.super_gluu.util.CertUtils;
import org.gluu.super_gluu.util.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import SuperGluu.app.BuildConfig;

/**
 * Service to process authentication/enrollment Fido U2F requests
 *
 * Created by Yuriy Movchan on 12/28/2015.
 */
public class SoftwareDevice {

    private static final String TAG = "software-device";

    public static final String SUPPORTED_U2F_VERSION = "U2F_V2";

    // Constants for ClientData.typ
    private static final String REQUEST_TYPE_REGISTER = "navigator.id.finishEnrollment";
    private static final String REQUEST_TYPE_AUTHENTICATE = "navigator.id.getAssertion";

    //for decline
    private static final String REGISTER_CANCEL_TYPE = "navigator.id.cancelEnrollment";
    private static final String AUTHENTICATE_CANCEL_TYPE = "navigator.id.cancelAssertion";

    // Constants for building ClientData.challenge
    private static final String JSON_PROPERTY_REQUEST_TYPE = "typ";
    private static final String JSON_PROPERTY_SERVER_CHALLENGE = "challenge";
    private static final String JSON_PROPERTY_SERVER_ORIGIN = "origin";
    public static final String JSON_PROPERTY_VERSION = "version";
    public static final String JSON_PROPERTY_APP_ID = "appId";
    public static final String JSON_PROPERTY_KEY_HANDLE = "keyHandle";

    private final Context context;

    private final RawMessageCodec rawMessageCodec;
    private final KeyPairGeneratorImpl keyPairGenerator;
    private U2FKeyImpl u2fKey;

    public SoftwareDevice(Context context, DataStore dataStore) {
        this.context = context;
        this.rawMessageCodec = new RawMessageCodecImpl();
        this.keyPairGenerator = new KeyPairGeneratorImpl();

        final X509Certificate vendorCertificate = CertUtils.loadHexEncodedCertificate(BuildConfig.VENDOR_CERTIFICATE_CERT);
        final PrivateKey vendorCertificatePrivateKey;
        try {
            vendorCertificatePrivateKey = keyPairGenerator.loadPrivateKey(BuildConfig.VENDOR_CERTIFICATE_PRIVATE_KEY);
        } catch (U2FException ex) {
            Log.e(TAG, "Failed to load vendor private key", ex);
            throw new RuntimeException("Failed to load vendor private key");
        }

        this.u2fKey = new U2FKeyImpl(
                vendorCertificate,
                vendorCertificatePrivateKey,
                this.keyPairGenerator,
                this.rawMessageCodec,
                dataStore,
                new UserPresenceVerifierImpl());
    }

    public TokenResponse enroll(String jsonRequest, OxPush2Request oxPush2Request, Boolean isDeny) throws JSONException, U2FException {
        JSONObject request = (JSONObject) new JSONTokener(jsonRequest).nextValue();

        if (request.has("registerRequests")) {
            JSONArray registerRequestArray = request.getJSONArray("registerRequests");
            if (registerRequestArray.length() == 0) {
                throw new U2FException("Failed to get registration request!");
            }
            request = (JSONObject) registerRequestArray.get(0);
        }

        if (!request.getString(JSON_PROPERTY_VERSION).equals(SUPPORTED_U2F_VERSION)) {
            throw new U2FException("Unsupported U2F_V2 version!");
        }

        String version = request.getString(JSON_PROPERTY_VERSION);
        String appParam = request.getString(JSON_PROPERTY_APP_ID);
        String challenge = request.getString(JSON_PROPERTY_SERVER_CHALLENGE);
        String origin = oxPush2Request.getIssuer();

        AndroidKeyDataStore androidKeyDataStore = new AndroidKeyDataStore(context);
        boolean isDuplicate = androidKeyDataStore.doesKeyAlreadyExist(oxPush2Request);

        EnrollmentResponse enrollmentResponse = u2fKey.register(new EnrollmentRequest(version, appParam, challenge, oxPush2Request));
        if (BuildConfig.DEBUG) Log.d(TAG, "Enrollment response: " + enrollmentResponse);

        JSONObject clientData = new JSONObject();
        if (isDeny){
            clientData.put(JSON_PROPERTY_REQUEST_TYPE, REGISTER_CANCEL_TYPE);
        } else {
            clientData.put(JSON_PROPERTY_REQUEST_TYPE, REQUEST_TYPE_REGISTER);
        }
        clientData.put(JSON_PROPERTY_SERVER_CHALLENGE, challenge);
        clientData.put(JSON_PROPERTY_SERVER_ORIGIN, origin);

        String clientDataString = clientData.toString();
        byte[] resp = rawMessageCodec.encodeRegisterResponse(enrollmentResponse);

        String deviceType = getDeviceType();
        String versionName = getVersionName();

        DeviceData deviceData = new DeviceData();
        deviceData.setUuid(DeviceUuidManager.getDeviceUuid(context).toString());
        deviceData.setPushToken(new AppFirebaseInstanceIDService().getPushRegistrationId(this.context));
        deviceData.setType(deviceType);
        deviceData.setPlatform("android");
        deviceData.setName(Build.MODEL);
        deviceData.setOsName(versionName);
        deviceData.setOsVersion(Build.VERSION.RELEASE);

        String deviceDataString = new Gson().toJson(deviceData);

        JSONObject response = new JSONObject();
        response.put("registrationData", Utils.base64UrlEncode(resp));
        response.put("clientData", Utils.base64UrlEncode(clientDataString.getBytes(Charset.forName("ASCII"))));
        response.put("deviceData", Utils.base64UrlEncode(deviceDataString.getBytes(Charset.forName("ASCII"))));


        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setResponse(response.toString());
        tokenResponse.setChallenge(new String(challenge));
        tokenResponse.setKeyHandle(new String(enrollmentResponse.getKeyHandle()));
        tokenResponse.setDuplicate(isDuplicate);

        return tokenResponse;
    }

    public TokenResponse sign(String jsonRequest, String origin, Boolean isDeny) throws JSONException, U2FException {
        if (BuildConfig.DEBUG) Log.d(TAG, "Starting to process sign request: " + jsonRequest);
        JSONObject request = (JSONObject) new JSONTokener(jsonRequest).nextValue();

        JSONArray authenticateRequestArray = null;
        if (request.has("authenticateRequests")) {
            authenticateRequestArray = request.getJSONArray("authenticateRequests");
            if (authenticateRequestArray.length() == 0) {
                throw new U2FException("Failed to get authentication request!");
            }
        } else {
            authenticateRequestArray = new JSONArray();
            authenticateRequestArray.put(request);
        }

        Log.i(TAG, "Found " + authenticateRequestArray.length() + " authentication requests");

        AuthenticateResponse authenticateResponse = null;
        String authenticatedChallenge = null;
        JSONObject authRequest = null;
        for (int i = 0; i < authenticateRequestArray.length(); i++) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Process authentication request: " + authRequest);
            authRequest = (JSONObject) authenticateRequestArray.get(i);

            if (!authRequest.getString(JSON_PROPERTY_VERSION).equals(SUPPORTED_U2F_VERSION)) {
                throw new U2FException("Unsupported U2F_V2 version!");
            }

            String version = authRequest.getString(JSON_PROPERTY_VERSION);
            String appParam = authRequest.getString(JSON_PROPERTY_APP_ID);
            String challenge = authRequest.getString(JSON_PROPERTY_SERVER_CHALLENGE);
            byte[] keyHandle = Base64.decode(authRequest.getString(JSON_PROPERTY_KEY_HANDLE), Base64.URL_SAFE | Base64.NO_WRAP);

            authenticateResponse = u2fKey.authenticate(new AuthenticateRequest(version, AuthenticateRequest.USER_PRESENCE_SIGN, challenge, appParam, keyHandle));
            if (BuildConfig.DEBUG) Log.d(TAG, "Authentication response: " + authenticateResponse);
            if (authenticateResponse != null) {
                authenticatedChallenge = challenge;
                break;
            }
        }

        if (authenticateResponse == null) {
            return null;
        }

        JSONObject clientData = new JSONObject();
        if (isDeny){
            clientData.put(JSON_PROPERTY_REQUEST_TYPE, AUTHENTICATE_CANCEL_TYPE);
        } else {
            clientData.put(JSON_PROPERTY_REQUEST_TYPE, REQUEST_TYPE_AUTHENTICATE);
        }
        clientData.put(JSON_PROPERTY_SERVER_CHALLENGE, authRequest.getString(JSON_PROPERTY_SERVER_CHALLENGE));
        clientData.put(JSON_PROPERTY_SERVER_ORIGIN, origin);

        String keyHandle = authRequest.getString(JSON_PROPERTY_KEY_HANDLE);
        String clientDataString = clientData.toString();
        byte[] resp = rawMessageCodec.encodeAuthenticateResponse(authenticateResponse);

        JSONObject response = new JSONObject();
        response.put("signatureData", Utils.base64UrlEncode(resp));
        response.put("clientData", Utils.base64UrlEncode(clientDataString.getBytes(Charset.forName("ASCII"))));
        response.put("keyHandle", keyHandle);

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setResponse(response.toString());
        tokenResponse.setChallenge(authenticatedChallenge);
        tokenResponse.setKeyHandle(keyHandle);

        return tokenResponse;
    }

    private String getDeviceType() {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        int modeType = uiModeManager.getCurrentModeType();
        switch (modeType) {
            case Configuration.UI_MODE_TYPE_NORMAL:
                return "normal";
            case Configuration.UI_MODE_TYPE_DESK:
                return "desk";
            case Configuration.UI_MODE_TYPE_CAR:
                return "car";
            case Configuration.UI_MODE_TYPE_TELEVISION:
                return "television";
            case Configuration.UI_MODE_TYPE_APPLIANCE:
                return "appliance";
            case Configuration.UI_MODE_TYPE_WATCH:
                return "watch";
        }

        // Cover unknown UI types
        return Integer.toString(modeType);
    }

    private String getVersionName() {
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (Exception ex) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Error: " + ex.getMessage());
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                return fieldName.toLowerCase();
            }
        }

        return "unknown";
    }

}
