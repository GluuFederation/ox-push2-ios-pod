/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.u2f.v2.store;

import org.gluu.super_gluu.app.model.LogInfo;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;

import java.util.List;

/**
 * Service to work with key pair store
 *
 * Created by Yuriy Movchan on 12/07/2015.
 */
public interface DataStore {

    void storeTokenEntry(TokenEntry tokenEntry);

    TokenEntry getTokenEntry(byte[] keyHandle);

    int incrementCounter(byte[] keyHandle);

    List<byte[]> getKeyHandlesByIssuerAndAppId(String application, String issuer);

    List<byte[]> getAllKeyHandles();

    List<String> getTokenEntries();

    void deleteTokenEntry(byte[] keyHandle);

    boolean doesKeyAlreadyExist(OxPush2Request oxPush2Request);

    //Methods for logs (save, get, delete, ....)

    void saveLog(LogInfo logText);
    List<LogInfo>getLogs();
    void deleteLogs();
    void deleteLogs(OxPush2Request... logInfo);
    void deleteLogs(List<LogInfo> logInfo);
    void changeKeyHandleName(TokenEntry tokenEntry, String newName);
    void deleteKeyHandle(TokenEntry tokenEntry);
}
