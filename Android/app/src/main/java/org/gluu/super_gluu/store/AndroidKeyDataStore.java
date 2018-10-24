/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.StringUtils;
import org.gluu.super_gluu.app.model.LogInfo;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;
import org.gluu.super_gluu.u2f.v2.store.DataStore;
import org.gluu.super_gluu.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import SuperGluu.app.BuildConfig;

/**
 * Provides methods to store key pair in application preferences
 *
 * Created by Yuriy Movchan on 12/28/2015.
 */
public class AndroidKeyDataStore implements DataStore {

    private static final String U2F_KEY_PAIR_FILE = "u2f_key_pairs";
    private static final String U2F_KEY_COUNT_FILE = "u2f_key_counts";
    private static final String LOGS_STORE = "logs_store";
    private static final String LOGS_KEY = "logs_key";

    private static final String TAG = "key-data-store";
    private final Context context;

    public AndroidKeyDataStore(Context context) {
        this.context = context;

        // Prepare empty U2F key pair store
        final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
        if (keySettings.getAll().size() == 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Creating empty U2K key pair store");
            keySettings.edit().commit();
        }

        // Prepare empty U2F key counter store
        final SharedPreferences keyCounts = context.getSharedPreferences(U2F_KEY_COUNT_FILE, Context.MODE_PRIVATE);
        if (keyCounts.getAll().size() == 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Creating empty U2K key counter store");
            keyCounts.edit().commit();
        }
    }

    @Override
    public void storeTokenEntry(TokenEntry tokenEntry) {
        Boolean isSave = true;
        List<String> tokens = getTokenEntries();
        for (String tokenStr : tokens){
            TokenEntry token = new Gson().fromJson(tokenStr, TokenEntry.class);
            if (token.getUserName().equalsIgnoreCase(tokenEntry.getUserName())){//token.getIssuer().equalsIgnoreCase(tokenEntry.getIssuer()
                isSave = false;
            }
        }
        if (isSave) {
            String keyHandleKey = keyHandleToKey(tokenEntry.getKeyHandle());

            final String tokenEntryString = new Gson().toJson(tokenEntry);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Storing new keyHandle: " + keyHandleKey + " with tokenEntry: " + tokenEntryString);

            final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);

            keySettings.edit().putString(keyHandleKey, tokenEntryString).commit();

            final SharedPreferences keyCounts = context.getSharedPreferences(U2F_KEY_COUNT_FILE, Context.MODE_PRIVATE);
            keyCounts.edit().putInt(keyHandleKey, 0).commit();
        }
    }

    @Override
    public TokenEntry getTokenEntry(byte[] keyHandle) {
        String keyHandleKey = keyHandleToKey(keyHandle);

        if (BuildConfig.DEBUG) Log.d(TAG, "Getting keyPair by keyHandle: " + keyHandleKey);

        final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
        final String tokenEntryString = keySettings.getString(keyHandleKey, null);

        if (BuildConfig.DEBUG) Log.d(TAG, "Found tokenEntry " + tokenEntryString + " by keyHandle: " + keyHandleKey);

        final TokenEntry tokenEntry = new Gson().fromJson(tokenEntryString, TokenEntry.class);

        return tokenEntry;
    }

    @Override
    public int incrementCounter(byte[] keyHandle) {
        String keyHandleKey = keyHandleToKey(keyHandle);

        if (BuildConfig.DEBUG) Log.d(TAG, "Incrementing keyHandle: " + keyHandleKey + " counter");

        final SharedPreferences keyCounts = context.getSharedPreferences(U2F_KEY_COUNT_FILE, Context.MODE_PRIVATE);

        int currentCounter = keyCounts.getInt(keyHandleKey, -1);
        currentCounter++;

        keyCounts.edit().putInt(keyHandleKey, currentCounter).commit();

        if (BuildConfig.DEBUG) Log.d(TAG, "Counter is " + currentCounter + " for keyHandle: " + keyHandleKey + " counter");

        return currentCounter;
    }

    @Override
    public List<byte[]> getKeyHandlesByIssuerAndAppId(String issuer, String application) {
        List<byte[]> result = new ArrayList<byte[]>();

        final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
        Map<String, String> keyTokens = (Map<String, String>) keySettings.getAll();
        for (Map.Entry<String, String> keyToken : keyTokens.entrySet()) {
            String tokenEntryString = keyToken.getValue();

            TokenEntry tokenEntry = new Gson().fromJson(tokenEntryString, TokenEntry.class);

            if (((issuer == null) || StringUtils.equals(issuer, tokenEntry.getIssuer()))
                    && ((application == null) || StringUtils.equals(application, tokenEntry.getApplication()))) {
                String keyHandleKey = keyToken.getKey();
                try {
                    byte[] keyHandle = keyToKeyHandle(keyHandleKey);
                    result.add(keyHandle);
                } catch (DecoderException ex) {
                    Log.e(TAG, "Invalid keyHandle: " + keyHandleKey, ex);
                }
            }
        }
        return result;
    }

    @Override
    public List<byte[]> getAllKeyHandles() {
        return getKeyHandlesByIssuerAndAppId(null, null);
    }

    @Override
    public List<String> getTokenEntries() {
        List<String> result = new ArrayList<String>();

        final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
        Map<String, String> keyTokens = (Map<String, String>) keySettings.getAll();
        for (Map.Entry<String, String> keyToken : keyTokens.entrySet()) {
            String tokenEntryString = keyToken.getValue();
            TokenEntry tokenEntry = new Gson().fromJson(tokenEntryString, TokenEntry.class);
            String keyHandleKey = keyToken.getKey();
            try {
                byte[] keyHandle = keyToKeyHandle(keyHandleKey);
                tokenEntry.setKeyHandle(keyHandle);
            } catch (DecoderException e) {
                Log.e(TAG, "Decoder exception: ", e);
            }
            tokenEntryString = new Gson().toJson(tokenEntry);
            result.add(tokenEntryString);
        }
        return result;
    }

    @Override
    public boolean doesKeyAlreadyExist(OxPush2Request oxPush2Request) {
        List<String> tokensString = getTokenEntries();
        if(tokensString == null || tokensString.isEmpty()) {
            return false;
        }

        List<TokenEntry> tokens = new ArrayList<TokenEntry>();
        for (String tokenString : tokensString){
            TokenEntry token = new Gson().fromJson(tokenString, TokenEntry.class);
            tokens.add(token);
        }

        for(TokenEntry tokenEntry: tokens) {
            if(tokenEntry != null && oxPush2Request != null &&
                    tokenEntry.getUserName().equals(oxPush2Request.getUserName()) &&
                    tokenEntry.getIssuer().equals(oxPush2Request.getIssuer())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void deleteTokenEntry(byte[] keyHandle) {
        String keyHandleKey = keyHandleToKey(keyHandle);
        final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
        final SharedPreferences keyCount = context.getSharedPreferences(U2F_KEY_COUNT_FILE, Context.MODE_PRIVATE);
        keySettings.edit().remove(keyHandleKey).commit();
        keyCount.edit().remove(keyHandleKey).commit();
    }

    @Override
    public void changeKeyHandleName(TokenEntry tokenEntry, String newName) {
        final SharedPreferences keySettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
        Map<String, String> keyTokens = (Map<String, String>) keySettings.getAll();
        for (Map.Entry<String, String> keyToken : keyTokens.entrySet()) {
            String tokenEntryString = keyToken.getValue();

            TokenEntry tokenEntryFromDB = new Gson().fromJson(tokenEntryString, TokenEntry.class);

            if (tokenEntryFromDB.getUserName().equalsIgnoreCase(tokenEntry.getUserName()) && tokenEntryFromDB.getIssuer().equalsIgnoreCase(tokenEntry.getIssuer())){//keyHandleID != null && StringUtils.equals(keyHandleID, tokenEntry.getIssuer())
                tokenEntry.setKeyName(newName);
                SharedPreferences tokenSettings = context.getSharedPreferences(U2F_KEY_PAIR_FILE, Context.MODE_PRIVATE);
                String tokenEntryStr = new Gson().toJson(tokenEntry);
                String keyHandleKey = keyHandleToKey(tokenEntry.getKeyHandle());
                tokenSettings.edit().putString(keyHandleKey, tokenEntryStr).commit();
                return;
            }
        }
    }

    @Override
    public void deleteKeyHandle(TokenEntry tokenEntry){
        this.deleteTokenEntry(tokenEntry.getKeyHandle());
    }

    private String keyHandleToKey(byte[] keyHandle) {
        return Utils.encodeHexString(keyHandle);
    }

    public byte[] keyToKeyHandle(String key) throws DecoderException {
        return Utils.decodeHexString(key);
    }

    //Methods for logs

    @Override
    public void saveLog(LogInfo logInfo) {
        final String logInfoString = new Gson().toJson(logInfo);
        final SharedPreferences logSettings = context.getSharedPreferences(LOGS_STORE, Context.MODE_PRIVATE);
        logSettings.edit().putString(UUID.randomUUID().toString(), logInfoString).commit();
    }

    @Override
    public List<LogInfo> getLogs() {
        final SharedPreferences logSettings = context.getSharedPreferences(LOGS_STORE, Context.MODE_PRIVATE);
        Map<String, String> logsMap = (Map<String, String>) logSettings.getAll();
        List<LogInfo> logs = new ArrayList<>();
        for (Map.Entry<String, String> log : logsMap.entrySet()) {
            logs.add(new Gson().fromJson(log.getValue(), LogInfo.class));
        }
        return logs;
    }

    @Override
    public void deleteLogs() {
        final SharedPreferences logSettings = context.getSharedPreferences(LOGS_STORE, Context.MODE_PRIVATE);
        logSettings.edit().clear().commit();
    }

    @Override
    public void deleteLogs(OxPush2Request... logInfo) {
        List<LogInfo> logsFromDB = this.getLogs();
        Iterator<LogInfo> iter = logsFromDB.iterator();
        for (OxPush2Request oxPush2Request : logInfo){
            while (iter.hasNext()) {
                LogInfo logInf = iter.next();
//            for (LogInfo logInf : logsFromDB) {
                if (oxPush2Request.getCreated().equalsIgnoreCase(logInf.getCreatedDate())) {
                    iter.remove();
//                    logsFromDB.remove(logInf);
                }
            }
        }
        logsFromDB.removeAll(Arrays.asList(logInfo));
        this.deleteLogs();
        for (LogInfo logInf : logsFromDB){
            this.saveLog(logInf);
        }
    }

    @Override
    public void deleteLogs(List<LogInfo> logInfo) {
        List<LogInfo> logsFromDB = this.getLogs();
        for (LogInfo oxPush2Request : logInfo){
            Iterator<LogInfo> iter = logsFromDB.iterator();
            while (iter.hasNext()) {
                LogInfo logInf = iter.next();
                if (oxPush2Request.getCreatedDate().equalsIgnoreCase(logInf.getCreatedDate())) {
                    iter.remove();
                }
            }
        }
        logsFromDB.removeAll(Arrays.asList(logInfo));
        this.deleteLogs();
        for (LogInfo logInf : logsFromDB){
            this.saveLog(logInf);
        }
    }

}