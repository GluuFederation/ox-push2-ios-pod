package org.gluu.super_gluu.app.listener;

/**
 * Created by SamIAm on 3/27/18.
 */

public interface EntrySelectedListener {

    void onPinCodeSelected();
    void setupFingerprintAuthentication();
    void startFingerprintAuthentication();
    void onSkipSelected();
}

