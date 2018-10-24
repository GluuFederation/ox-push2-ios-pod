package org.gluu.super_gluu.app.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.gluu.super_gluu.app.fragment.SettingsFragment;

import java.util.Date;
import java.util.Map;

/**
 * Created by nazaryavornytskyy on 7/12/16.
 */
public class Settings {

    private Boolean isEditingModeLogs = false;

    public static boolean isAuthEnabled(Context context) {
        return getFingerprintEnabled(context) || getPinCodeEnabled(context) || isAppLocked(context);
    }

    //Pin code Settings
    public static void setPinCodeEnabled(Context context, Boolean isEnabled) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constant.IS_PIN_ENABLED, isEnabled);
        editor.commit();
    }

    public static Boolean getPinCodeEnabled(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        Boolean isPinEnabled = preferences.getBoolean(Constant.IS_PIN_ENABLED, false);
        return isPinEnabled;
    }

    public static String getPinCode(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        String pinCode = preferences.getString(Constant.PIN_CODE, null);
        return pinCode;
    }

    public static void savePinCode(Context context, String password){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.PIN_CODE, password);
        editor.commit();
    }

    public static void clearPinCode(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.PIN_CODE, null);
        editor.commit();
    }

    public static int getCurrentPinCodeAttempts(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        String pinCode = preferences.getString(Constant.CURRENT_PIN_CODE_ATTEMPTS, String.valueOf(Constant.PIN_CODE_ATTEMPTS_VALUE));
        return Integer.parseInt(pinCode);
    }

    public static void setCurrentPinCodeAttempts(Context context, int attempts){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.CURRENT_PIN_CODE_ATTEMPTS, String.valueOf(attempts));
        editor.commit();
    }


    public static void resetCurrentPinAttempts(Context context){
        Settings.saveIsReset(context);
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.CURRENT_PIN_CODE_ATTEMPTS, String.valueOf(Constant.PIN_CODE_ATTEMPTS_VALUE));
        editor.commit();
    }

    public static void saveIsReset(Context context){
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isReset", true);
            editor.commit();
        }
    }

    public static void setAppLocked(Context context, Boolean isLocked){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constant.APP_LOCKED, isLocked);
        editor.commit();
    }

    public static Boolean isAppLocked(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        return preferences.getBoolean(Constant.APP_LOCKED, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setAppLockedTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Date date = addMinutesToDate(Constant.APP_LOCKED_MINUTES, new Date(System.currentTimeMillis()));
        editor.putLong(Constant.APP_LOCKED_TIME, date.getTime());
        editor.commit();
    }

    public static Long getAppLockedTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        return preferences.getLong(Constant.APP_LOCKED_TIME, 0);
    }

    @SuppressLint("ApplySharedPref")
    public static void clearAppLockedTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(Constant.APP_LOCKED_TIME, 0);
        editor.commit();
    }

    //todo delete both save accept and get accept if it is confirmed there is no plan to force user to accept license
//    public static void saveAccept(Context context){
//        SharedPreferences preferences = context.getSharedPreferences("IsAcceptLicense", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = preferences.edit();
//        editor.putBoolean("isAccept", true);
//        editor.commit();
//    }
//
//    public static Boolean getAccept(Context context){
//        SharedPreferences preferences = context.getSharedPreferences("IsAcceptLicense", Context.MODE_PRIVATE);
//        Boolean isAccept = preferences.getBoolean("isAccept", false);
//        return isAccept;
//    }

    public static Boolean getFirstLoad(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        Boolean isFirstLoad = preferences.getBoolean("isFirstLoad", false);
        return !isFirstLoad;
    }

    public static void saveFirstLoad(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Constant.PIN_CODE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isFirstLoad", true);
        editor.commit();
    }

    //END Pin code Settings

    //region Push Notification/Auth Request Settings

    @SuppressLint("ApplySharedPref")
    public static void setPushOxData(Context context, String pushData) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.OX_REQUEST_DATA, pushData);
        editor.commit();
    }

    public static String getOxRequestData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        return preferences.getString(Constant.OX_REQUEST_DATA, null);
    }

    @SuppressLint("ApplySharedPref")
    public static void setPushOxRequestTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Date date = new Date(System.currentTimeMillis());
        editor.putLong(Constant.OX_REQUEST_RECEIVED_TIME, date.getTime());
        editor.commit();
    }

    public static Long getOxRequestTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        return preferences.getLong(Constant.OX_REQUEST_RECEIVED_TIME, 0);
    }

    @SuppressLint("ApplySharedPref")
    private static void clearPushOxRequestTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(Constant.OX_REQUEST_RECEIVED_TIME, 0);
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void setUserChoice(Context context, String userChoice) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constant.USER_CHOICE, userChoice);
        editor.apply();
    }

    public static String getUserChoice(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        return preferences.getString(Constant.USER_CHOICE, null);
    }

    public static boolean isAuthPending(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Constant.OX_PUSH_SETTINGS, Context.MODE_PRIVATE);
        return preferences.getString(Constant.OX_REQUEST_DATA, null) != null;
    }

    public static void clearPushOxData(Context context) {
        setPushOxData(context, null);
        setUserChoice(context,null);
        clearPushOxRequestTime(context);
    }

    //endregion

    //SSL Connection Settings
    public static void setSSLEnabled(Context context, Boolean isEnabled) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsFragment.Constant.SSL_CONNECTION_TYPE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SettingsFragment.Constant.SSL_CONNECTION_TYPE, isEnabled);
        editor.apply();
    }

    public static Boolean getSSLEnabled(Context context){
        SharedPreferences preferences = context.getSharedPreferences(SettingsFragment.Constant.SSL_CONNECTION_TYPE, Context.MODE_PRIVATE);
        Boolean isSSLEnabled = preferences.getBoolean(SettingsFragment.Constant.SSL_CONNECTION_TYPE, false);
        return isSSLEnabled;
    }

    //Fingerprint settings
    public static void setFingerprintEnabled(Context context, Boolean isEnabled) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsFragment.Constant.FINGERPRINT_TYPE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SettingsFragment.Constant.FINGERPRINT_TYPE, isEnabled);
        editor.apply();
    }

    public static Boolean getFingerprintEnabled(Context context){
        SharedPreferences preferences = context.getSharedPreferences(SettingsFragment.Constant.FINGERPRINT_TYPE, Context.MODE_PRIVATE);
        Boolean isFingerprintEnabled = preferences.getBoolean(SettingsFragment.Constant.FINGERPRINT_TYPE, false);
        return isFingerprintEnabled;
    }

    public static Boolean getSettingsValueEnabled(Context context, String key){
        SharedPreferences preferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        Boolean isValue = preferences.getBoolean(key, false);
        return isValue;
    }

    public static void setSettingsValueEnabled(Context context, String key, Boolean isEnabled) {
        SharedPreferences preferences = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, isEnabled);
        editor.apply();
    }

    //For purchases
    public static void setPurchase(Context context, Boolean isEnabled) {
        SharedPreferences preferences = context.getSharedPreferences("PurchaseSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isPurchased", isEnabled);
        editor.apply();
    }

    public static Boolean getPurchase(Context context){
        SharedPreferences preferences = context.getSharedPreferences("PurchaseSettings", Context.MODE_PRIVATE);
        Boolean isFingerprintEnabled = preferences.getBoolean("isPurchased", false);
        return isFingerprintEnabled;
    }

    public static void updateLicense(Context context, String licenseId, boolean isLicensed) {
        SharedPreferences licensePrefs = context.getSharedPreferences(Constant.LICENSE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = licensePrefs.edit();
        editor.putBoolean(licenseId, isLicensed);
        editor.apply();
    }

    public static void removeLicense(Context context, String licenseId) {
        SharedPreferences licensePrefs = context.getSharedPreferences(Constant.LICENSE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = licensePrefs.edit();
        editor.remove(licenseId);
        editor.apply();
    }

    public static boolean isLicensed(Context context) {
        SharedPreferences licensePrefs = context.getSharedPreferences(Constant.LICENSE_SETTINGS, Context.MODE_PRIVATE);
        Map<String, ?> allLicenseEntries = licensePrefs.getAll();

        if(allLicenseEntries.isEmpty()) {
            return false;
        }

        for(Map.Entry<String, ?> entry : allLicenseEntries.entrySet()) {
            Boolean licensed = licensePrefs.getBoolean(entry.getKey(), false);
            if(licensed) {
                return true;
            }
        }
        return false;
    }

    //For actions bar menu
    public static Boolean getIsSettingsMenuVisible(Context context){
        SharedPreferences preferences = context.getSharedPreferences("CleanLogsSettings", Context.MODE_PRIVATE);
        Boolean isVisible = preferences.getBoolean("isCleanButtonVisible", false);
        return isVisible;
    }

    public static void setIsSettingsMenuVisible(Context context, Boolean isVisible){
        SharedPreferences preferences = context.getSharedPreferences("CleanLogsSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isCleanButtonVisible", isVisible);
        editor.apply();
        editor.commit();
    }

    public static Boolean getIsBackButtonVisibleForKey(Context context){
        SharedPreferences preferences = context.getSharedPreferences("CleanLogsSettings", Context.MODE_PRIVATE);
        Boolean isVisible = preferences.getBoolean("isBackButtonVisibleForKey", false);
        return isVisible;
    }

    public static void setIsBackButtonVisibleForKey(Context context, Boolean isVsible){
        SharedPreferences preferences = context.getSharedPreferences("CleanLogsSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isBackButtonVisibleForKey", isVsible);
        editor.apply();
    }

    public static Boolean getIsBackButtonVisibleForLog(Context context){
        SharedPreferences preferences = context.getSharedPreferences("CleanLogsSettings", Context.MODE_PRIVATE);
        Boolean isVisible = preferences.getBoolean("isBackButtonVisibleForLog", false);
        return isVisible;
    }

    public static void setIsBackButtonVisibleForLog(Context context, Boolean isVsible){
        SharedPreferences preferences = context.getSharedPreferences("CleanLogsSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isBackButtonVisibleForLog", isVsible);
        editor.apply();
    }

    public Boolean getEditingModeLogs() {
        return isEditingModeLogs;
    }

    public void setEditingModeLogs(Boolean editingModeLogs) {
        isEditingModeLogs = editingModeLogs;
    }

    private static Date addMinutesToDate(int minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;

        long curTimeInMs = beforeTime.getTime();
        return new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
    }

    public static class Constant {
        private static final String PIN_CODE = "PinCode";
        private static final String IS_PIN_ENABLED = "isPinEnabled";
        private static final String PIN_CODE_ATTEMPTS = "pinCodeAttempts";
        private static final String CURRENT_PIN_CODE_ATTEMPTS = "currentPinCodeAttempts";
        private static final String PIN_CODE_SETTINGS = "PinCodeSettings";


        private static final String OX_PUSH_SETTINGS = "oxPushSettings";
        private static final String USER_CHOICE = "UserChoice";
        private static final String OX_REQUEST_DATA = "OxRequestData";
        private static final String OX_REQUEST_RECEIVED_TIME = "OxRequestReceievedTime";

        private static final String LICENSE_SETTINGS = "license_settings";

        private static final String APP_LOCKED = "isAppLocked";
        private static final String APP_LOCKED_TIME = "appLockedTimeLong";

        public static final int APP_LOCKED_MINUTES = 10;

        private static final int PIN_CODE_ATTEMPTS_VALUE = 5;


        public static final int AUTH_VALID_TIME = 60;
    }
}
