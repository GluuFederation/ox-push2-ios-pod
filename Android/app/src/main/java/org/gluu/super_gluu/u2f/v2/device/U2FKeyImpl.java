/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.u2f.v2.device;

import android.util.Log;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.gluu.super_gluu.app.GluuApplication;
import org.gluu.super_gluu.u2f.v2.cert.KeyPairGenerator;
import org.gluu.super_gluu.u2f.v2.codec.RawMessageCodec;
import org.gluu.super_gluu.u2f.v2.exception.U2FException;
import org.gluu.super_gluu.u2f.v2.model.AuthenticateRequest;
import org.gluu.super_gluu.u2f.v2.model.AuthenticateResponse;
import org.gluu.super_gluu.u2f.v2.model.EnrollmentRequest;
import org.gluu.super_gluu.u2f.v2.model.EnrollmentResponse;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;
import org.gluu.super_gluu.u2f.v2.store.DataStore;
import org.gluu.super_gluu.u2f.v2.user.UserPresenceVerifier;
import org.gluu.super_gluu.util.Utils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import SuperGluu.app.BuildConfig;
import SuperGluu.app.R;

/**
 * Fido U2F key service to process enrollment/authentication request
 *
 * Created by Yuriy Movchan on 12/28/2015.
 */
public class U2FKeyImpl implements U2FKey {

    private static final String TAG = "key-data-store";

    private final X509Certificate vendorCertificate;
    private final PrivateKey certificatePrivateKey;
    private final KeyPairGenerator keyPairGenerator;
    private final RawMessageCodec rawMessageCodec;
    private final DataStore dataStore;
    private final UserPresenceVerifier userPresenceVerifier;

    public U2FKeyImpl(X509Certificate vendorCertificate, PrivateKey certificatePrivateKey,
                      KeyPairGenerator keyPairGenerator, RawMessageCodec rawMessageCodec,
                      DataStore dataStore, UserPresenceVerifier userPresenceVerifier) {
        this.vendorCertificate = vendorCertificate;
        this.certificatePrivateKey = certificatePrivateKey;
        this.keyPairGenerator = keyPairGenerator;
        this.rawMessageCodec = rawMessageCodec;
        this.dataStore = dataStore;
        this.userPresenceVerifier = userPresenceVerifier;
    }

    @Override
    public EnrollmentResponse register(EnrollmentRequest enrollmentRequest) throws U2FException {
        if (BuildConfig.DEBUG) Log.d(TAG, ">> register");

        String application = enrollmentRequest.getApplication();
        String challenge = enrollmentRequest.getChallenge();

        if (BuildConfig.DEBUG) Log.d(TAG, "-- Inputs --");
        if (BuildConfig.DEBUG) Log.d(TAG, "application: " + application);
        if (BuildConfig.DEBUG) Log.d(TAG, "challenge: " + challenge);

        byte userPresent = userPresenceVerifier.verifyUserPresence();
        if ((userPresent & AuthenticateRequest.USER_PRESENT_FLAG) == 0) {
            throw new U2FException("Cannot verify user presence");
        }

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        byte[] keyHandle = keyPairGenerator.generateKeyHandle();

        TokenEntry tokenEntry = new TokenEntry(keyPairGenerator.keyPairToJson(keyPair), enrollmentRequest.getApplication(), enrollmentRequest.getOxPush2Request().getIssuer());
        tokenEntry.setCreatedDate(enrollmentRequest.getOxPush2Request().getCreated());
        tokenEntry.setUserName(enrollmentRequest.getOxPush2Request().getUserName());
        tokenEntry.setAuthenticationMode(enrollmentRequest.getOxPush2Request().getMethod());
        tokenEntry.setKeyHandle(keyHandle);
        String keyHandleTitle = tokenEntry.getIssuer();;
        tokenEntry.setKeyName(keyHandleTitle);
        final boolean oneStep = Utils.isEmpty(enrollmentRequest.getOxPush2Request().getUserName());
        String authenticationType = GluuApplication.get().getString(oneStep ? R.string.one_step : R.string.two_step);
        tokenEntry.setAuthenticationType(authenticationType);
        dataStore.storeTokenEntry(tokenEntry);

        byte[] userPublicKey = keyPairGenerator.encodePublicKey(keyPair.getPublic());

        byte[] applicationSha256 = DigestUtils.sha256(application);
        byte[] challengeSha256 = DigestUtils.sha256(challenge);
        byte[] signedData = rawMessageCodec.encodeRegistrationSignedBytes(applicationSha256, challengeSha256, keyHandle, userPublicKey);
        if (BuildConfig.DEBUG) Log.d(TAG, "Signing bytes " + Utils.encodeHexString(signedData));

        byte[] signature = keyPairGenerator.sign(signedData, certificatePrivateKey);

        if (BuildConfig.DEBUG) Log.d(TAG, "-- Outputs --");
        if (BuildConfig.DEBUG) Log.d(TAG, "userPublicKey: " + Utils.encodeHexString(userPublicKey));
        if (BuildConfig.DEBUG) Log.d(TAG, "keyHandle: " + Utils.base64UrlEncode(keyHandle));
        if (BuildConfig.DEBUG) Log.d(TAG, "vendorCertificate: " + vendorCertificate);
        if (BuildConfig.DEBUG) Log.d(TAG, "signature: " + Utils.encodeHexString(signature));

        if (BuildConfig.DEBUG) Log.d(TAG, "<< register");

        return new EnrollmentResponse(userPublicKey, keyHandle, vendorCertificate, signature);
    }

    @Override
    public AuthenticateResponse authenticate(AuthenticateRequest authenticateRequest)
            throws U2FException {
        if (BuildConfig.DEBUG) Log.d(TAG, ">> authenticate");

        byte control = authenticateRequest.getControl();
        String application = authenticateRequest.getApplication();
        String challenge = authenticateRequest.getChallenge();
        byte[] keyHandle = authenticateRequest.getKeyHandle();

        if (BuildConfig.DEBUG) Log.d(TAG, "-- Inputs --");
        if (BuildConfig.DEBUG) Log.d(TAG, "control: " + control);
        if (BuildConfig.DEBUG) Log.d(TAG, "application: " + application);
        if (BuildConfig.DEBUG) Log.d(TAG, "challenge: " + challenge);
        if (BuildConfig.DEBUG) Log.d(TAG, "keyHandle: " + Utils.base64UrlEncode(keyHandle));

        TokenEntry tokenEntry = dataStore.getTokenEntry(keyHandle);

        if (tokenEntry == null) {
            Log.e(TAG, "There is no keyPair for keyHandle: " + Utils.base64UrlEncode(keyHandle));
            return null;
        }

        if (!StringUtils.equals(application, tokenEntry.getApplication())) {
            throw new U2FException("KeyHandle " + Utils.base64UrlEncode(keyHandle) + " is associated with application: " + tokenEntry.getApplication());
        }

        String keyPairJson = tokenEntry.getKeyPair();
        if (keyPairJson == null) {
            Log.e(TAG, "There is no keyPair for keyHandle: " + Utils.base64UrlEncode(keyHandle));
            return null;
        }

        KeyPair keyPair;
        try {
            keyPair = keyPairGenerator.keyPairFromJson(keyPairJson);
        } catch (U2FException ex) {
            Log.e(TAG, "There is no keyPair for keyHandle: " + Utils.base64UrlEncode(keyHandle));
            return null;
        }

        int counter = dataStore.incrementCounter(keyHandle);
        byte userPresence = userPresenceVerifier.verifyUserPresence();
        byte[] applicationSha256 = DigestUtils.sha256(application);
        byte[] challengeSha256 = DigestUtils.sha256(challenge);
        byte[] signedData = rawMessageCodec.encodeAuthenticateSignedBytes(applicationSha256, userPresence,
                counter, challengeSha256);

        if (BuildConfig.DEBUG) Log.d(TAG, "Signing bytes " + Utils.encodeHexString(signedData));

        byte[] signature = keyPairGenerator.sign(signedData, keyPair.getPrivate());

        if (BuildConfig.DEBUG) Log.d(TAG, "-- Outputs --");
        if (BuildConfig.DEBUG) Log.d(TAG, "userPresence: " + userPresence);
        if (BuildConfig.DEBUG) Log.d(TAG, "counter: " + counter);
        if (BuildConfig.DEBUG) Log.d(TAG, "signature: " + Utils.encodeHexString(signature));

        if (BuildConfig.DEBUG) Log.d(TAG, "<< authenticate");

        return new AuthenticateResponse(userPresence, counter, signature);
    }

}
