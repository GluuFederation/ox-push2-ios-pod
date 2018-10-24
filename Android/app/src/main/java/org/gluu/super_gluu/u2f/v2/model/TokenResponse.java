/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.u2f.v2.model;

/**
 * oxAuth Fido U2F auth/enroll response
 *
 * Created by Yuriy Movchan on 01/08/2016.
 */
public class TokenResponse {

    private String response;
    private String challenge;
    private String keyHandle;
    private boolean isDuplicate;

    public TokenResponse() {
    }

    public TokenResponse(String response, String challenge, String keyHandle, boolean isDuplicate) {
        this.response = response;
        this.challenge = challenge;
        this.keyHandle = keyHandle;
        this.isDuplicate = isDuplicate;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getKeyHandle() {
        return keyHandle;
    }

    public void setKeyHandle(String keyHandle) {
        this.keyHandle = keyHandle;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }
}
