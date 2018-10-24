/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.app.activities;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;

import org.gluu.super_gluu.app.GluuApplication;
import org.gluu.super_gluu.app.ProcessManager;
import org.gluu.super_gluu.app.customview.CustomAlert;
import org.gluu.super_gluu.app.customview.CustomToast;
import org.gluu.super_gluu.app.fragment.HomeFragment;
import org.gluu.super_gluu.app.fragment.KeyFragmentListFragment;
import org.gluu.super_gluu.app.fragment.KeyHandleInfoFragment;
import org.gluu.super_gluu.app.fragment.LicenseFragment;
import org.gluu.super_gluu.app.fragment.LogsFragment;
import org.gluu.super_gluu.app.fragment.PinCodeFragment;
import org.gluu.super_gluu.app.fragment.RequestDetailFragment;
import org.gluu.super_gluu.app.fragment.SettingsFragment;
import org.gluu.super_gluu.app.fragment.SettingsPinCode;
import org.gluu.super_gluu.app.listener.OxPush2RequestListener;
import org.gluu.super_gluu.app.model.LogInfo;
import org.gluu.super_gluu.app.purchase.InAppPurchaseService;
import org.gluu.super_gluu.app.settings.Settings;
import org.gluu.super_gluu.device.DeviceUuidManager;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.net.CommunicationService;
import org.gluu.super_gluu.store.AndroidKeyDataStore;
import org.gluu.super_gluu.u2f.v2.SoftwareDevice;
import org.gluu.super_gluu.u2f.v2.exception.U2FException;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;
import org.gluu.super_gluu.u2f.v2.model.TokenResponse;
import org.gluu.super_gluu.u2f.v2.store.DataStore;
import org.gluu.super_gluu.util.Utils;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import SuperGluu.app.BuildConfig;
import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Main activity
 *
 * Created by Yuriy Movchan on 12/28/2015.
 */
public class MainNavDrawerActivity extends BaseActivity
        implements OxPush2RequestListener, KeyHandleInfoFragment.OnDeleteKeyHandleListener,
        PinCodeFragment.PinCodeViewListener, RequestDetailFragment.OnDeleteLogInfoListener,
        HomeFragment.GluuAdListener {

    //region class variables

    private static final String TAG = "main-activity";

    /**
     * Id to identify a camera permission request.
     */
    private static final int REQUEST_CAMERA = 0;

    public static final String QR_CODE_PUSH_NOTIFICATION_MESSAGE = MainNavDrawerActivity.class.getPackage().getName() + ".QR_CODE_PUSH_NOTIFICATION_MESSAGE";
    public static final String VIBRATE_AND_RINGTONE = "VIBRATE_AND_PLAY_RINGTONE";
    public static final String QR_CODE_PUSH_NOTIFICATION = "QR_CODE_PUSH_NOTIFICATION";
    public static final int MESSAGE_NOTIFICATION_ID = 444555;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nvView)
    NavigationView navigationView;
    @BindView(R.id.nav_drawer_toolbar)
    Toolbar toolbar;

    private ActionBarDrawerToggle toggle;

    private SoftwareDevice u2f;
    private AndroidKeyDataStore dataStore;
    private Context context;

    private FragmentManager fragmentManager;

    private Settings settings = new Settings();

    private InterstitialAd interstitialAd;

    private BroadcastReceiver mPushMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Pop backstack to get back to home screen
            if(fragmentManager.getBackStackEntryCount() > 0) {
                for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                    fragmentManager.popBackStack();
                }
            }

            boolean vibrateAndPlayRingtone = intent.getBooleanExtra(MainNavDrawerActivity.VIBRATE_AND_RINGTONE, true);

            // Get extra data included in the Intent
            String message = intent.getStringExtra(MainNavDrawerActivity.QR_CODE_PUSH_NOTIFICATION_MESSAGE);
            final OxPush2Request oxPush2Request = new Gson().fromJson(message, OxPush2Request.class);
            onQrRequest(oxPush2Request);
            //play sound and vibrate
            if(vibrateAndPlayRingtone) {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(context, notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Vibrator vibrator = (Vibrator) getApplication().getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator != null) {
                    vibrator.vibrate(800);
                }
            }
        }
    };

    //For purchases
    private InAppPurchaseService inAppPurchaseService = new InAppPurchaseService();

    //endregion

    //region lifecycle and activity methods
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_drawer_main);
        ButterKnife.bind(this);
        context = getApplicationContext();

        fragmentManager = getSupportFragmentManager();
        setupToolbar();
        initNavDrawer();

        // Init network layer
        CommunicationService.init();

        // Init device UUID service
        DeviceUuidManager deviceUuidFactory = new DeviceUuidManager();
        deviceUuidFactory.init(this);

        this.dataStore = new AndroidKeyDataStore(context);
        this.u2f = new SoftwareDevice(this, dataStore);

        checkUserCameraPermission();

        //temporary turn off rotation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        //Init InAPP-Purchase service
        initIAPurchaseService();

        setupInterstitialAd();

        setupInitialFragment();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(mPushMessageReceiver,
                new IntentFilter(MainNavDrawerActivity.QR_CODE_PUSH_NOTIFICATION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPushMessageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!GluuApplication.didAppGoThroughLauncherActivity()
                && Settings.isAuthEnabled(MainNavDrawerActivity.this)) {
            newTaskToEntryActivity();
        } else {
            //Check push data
            checkIsPush();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (inAppPurchaseService.isHandleResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if(fragmentManager != null) {
            Fragment fragment = fragmentManager.findFragmentById(R.id.main_frame_layout);
            if(fragment instanceof RequestDetailFragment) {
                if(((RequestDetailFragment)fragment).getUserInfo()) {
                    super.onBackPressed();
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        inAppPurchaseService.deInitPurchaseService();
        super.onDestroy();
    }
    //endregion

    //region initialization methods
    private void setupInitialFragment() {
        HomeFragment homeFragment = new HomeFragment();

        //Insert the fragment by replacing any existing fragment
        fragmentManager
                .beginTransaction()
                .replace(R.id.main_frame_layout, homeFragment)
                .commit();

    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        setTitle("");
    }

    private void initNavDrawer() {
        navigationView.setItemIconTintList(null);

        setupDrawerContent(navigationView);

        setupToggleState();
    }

    private void initGoogleADS(Boolean isShow){
        Intent intent = new Intent("on-ad-free-event");
        intent.putExtra("isAdFree", isShow);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void initIAPurchaseService(){
        inAppPurchaseService.initInAppService(context);
        //Init GoogleMobile AD
        inAppPurchaseService.setCustomEventListener(isSubscribed -> initGoogleADS(isSubscribed));
        inAppPurchaseService.reloadPurchaseService();
    }

    private void checkUserCameraPermission(){
        Log.i(TAG, "Show camera button pressed. Checking permission.");
        // Check if the Camera permission is already available.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Camera permission has not been granted.
            showCameraMessagingAlertView();

        } else {
            // Camera permissions is already available, show the camera preview.
            Log.i(TAG,
                    "CAMERA permission has already been granted. Displaying camera preview.");
        }
    }

    private void setupInterstitialAd(){

        if(!areAdsDisabled()) {
            interstitialAd = new InterstitialAd(MainNavDrawerActivity.this);
            interstitialAd.setAdUnitId(BuildConfig.INTERSTITIAL_AD_ID);
            final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                }
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    if(interstitialAd != null) {
                        interstitialAd.loadAd(adRequestBuilder.build());
                    }
                }
            });
            interstitialAd.loadAd(adRequestBuilder.build());
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            selectDrawerItem(menuItem);
            return true;
        });
    }

    private void setupToggleState() {
        if (toolbar != null) {
            toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
            toggle.syncState();
            drawer.addDrawerListener(toggle);

            OnBackStackChangedListener onBackStackChangedListener = () -> {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                } else {
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                }
            };

            fragmentManager.addOnBackStackChangedListener(onBackStackChangedListener);
        }

    }

    public void checkIsPush(){
        if(Settings.isAuthPending(getBaseContext())) {
            if(getTimeDifferenceInSeconds() < Settings.Constant.AUTH_VALID_TIME) {
                makeOxRequest();
            } else {
                Settings.clearPushOxData(context);
            }
        }
    }

    //endregion

    //region interface implementations
    @Override
    public void onQrRequest(final OxPush2Request oxPush2Request) {
        if (!this.isDestroyed()) {
            doQrRequest(oxPush2Request);
        }
    }

    @Override
    public void onAdFreeButtonClick(){
        if (inAppPurchaseService.readyToPurchase) {
            if (!inAppPurchaseService.isSubscribed) {
                inAppPurchaseService.purchase(MainNavDrawerActivity.this);
            } else {
                initGoogleADS(true);
            }
        }
    }

    @Override
    public void onPurchaseRestored() {
        if (inAppPurchaseService.readyToPurchase) {
            if (!inAppPurchaseService.isSubscribed) {
                inAppPurchaseService.restorePurchase();
            } else {
                initGoogleADS(true);
            }
        }
    }

    @Override
    public TokenResponse onSign(String jsonRequest, String origin, Boolean isDeny)
            throws JSONException, IOException, U2FException {
        return u2f.sign(jsonRequest, origin, isDeny);
    }

    @Override
    public TokenResponse onEnroll(String jsonRequest, OxPush2Request oxPush2Request, Boolean isDeny)
            throws JSONException, IOException, U2FException {
        return u2f.enroll(jsonRequest, oxPush2Request, isDeny);
    }

    @Override
    public DataStore onGetDataStore() {
        return dataStore;
    }

    @Override
    public void onDeleteLogInfo(OxPush2Request oxPush2Request) {
        dataStore.deleteLogs(oxPush2Request);
        onBackButtonClicked();
    }

    @Override
    public void onDeleteLogInfo(List<LogInfo> logInfo) {
        dataStore.deleteLogs(logInfo);
        settings.setEditingModeLogs(false);
        reloadLogs();
    }

    @Override
    public void showInterstitialAd() {
        if(interstitialAd == null) {
            setupInterstitialAd();
        } else if(interstitialAd.isLoaded()) {
            interstitialAd.show();
        }
    }

    @Override
    public boolean areAdsDisabled() {
        return !getResources().getBoolean(R.bool.adsEnabled) ||
                Settings.isLicensed(context) ||
                Settings.getPurchase(context);
    }

    @Override
    public void onDeleteKeyHandle(TokenEntry key) {
        dataStore.deleteKeyHandle(key);
    }

    @Override
    public void onCorrectPinCode(boolean isPinCodeCorrect) {
        if (isPinCodeCorrect){
            showAlertView(getString(R.string.pin_code), getString(R.string.new_pin_added));
        } else {
            //to change pin code, first need check if user knows current one
            Intent intent = new Intent(MainNavDrawerActivity.this, EntryActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onCancel(PinCodeFragment.EntryType entryType) {

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }

        if(entryType == PinCodeFragment.EntryType.SETTING_NEW) {
            Settings.setPinCodeEnabled(getBaseContext(), false);
            Settings.clearPinCode(getBaseContext());
        }

        onBackPressed();
    }

    //endregion

    //region general helper methods

    private void reloadLogs(){
        Intent intent = new Intent("reload-logs");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onBackButtonClicked(){
        if (Settings.getIsBackButtonVisibleForLog(getApplicationContext())) {
            Settings.setIsBackButtonVisibleForLog(getApplicationContext(), false);
            invalidateOptionsMenu();
            onBackPressed();
        } else {
            Settings.setIsBackButtonVisibleForLog(getApplicationContext(), false);
            if (settings.getEditingModeLogs()) {
                Intent intent = new Intent("on-delete-logs");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                settings.setEditingModeLogs(false);
            } else {
                settings.setEditingModeLogs(true);
            }
            Intent intent = new Intent("editing-mode-logs");
            intent.putExtra("isEditingMode", settings.getEditingModeLogs());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            invalidateOptionsMenu();
        }
    }

    public void doQrRequest(OxPush2Request oxPush2Request) {
        if (!validateOxPush2Request(oxPush2Request)) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager != null) {
            notificationManager.cancel(MainNavDrawerActivity.MESSAGE_NOTIFICATION_ID);
        }
        final ProcessManager processManager = createProcessManager(oxPush2Request);

        RequestProcessListener requestProcessListener = new RequestProcessListener() {
            @Override
            public void onApprove() {
                String licenseId = OxPush2Request.getLicenseId(oxPush2Request.getIssuer(), oxPush2Request.getUserName());
                Settings.updateLicense(context, licenseId, oxPush2Request.isLicensed());
                Settings.clearPushOxData(getApplicationContext());
                processManager.onOxPushRequest(false);
            }

            @Override
            public void onDeny() {
                Settings.clearPushOxData(getApplicationContext());
                processManager.onOxPushRequest(true);
            }
        };

        RequestDetailFragment requestDetailFragment =
                RequestDetailFragment.newInstance(false, null, oxPush2Request, requestProcessListener);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frame_layout, requestDetailFragment);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    private ProcessManager createProcessManager(OxPush2Request oxPush2Request){
        ProcessManager processManager = new ProcessManager();
        processManager.setOxPush2Request(oxPush2Request);
        processManager.setDataStore(dataStore);
        processManager.setActivity(this);
        processManager.setOxPush2RequestListener(new OxPush2RequestListener() {
            @Override
            public void onQrRequest(OxPush2Request oxPush2Request) {
                //skip code there
            }

            @Override
            public TokenResponse onSign(String jsonRequest, String origin, Boolean isDeny)
                    throws JSONException, IOException, U2FException {
                return u2f.sign(jsonRequest, origin, isDeny);
            }

            @Override
            public TokenResponse onEnroll(String jsonRequest, OxPush2Request oxPush2Request, Boolean isDeny)
                    throws JSONException, IOException, U2FException {
                return u2f.enroll(jsonRequest, oxPush2Request, isDeny);
            }

            @Override
            public DataStore onGetDataStore() {
                return dataStore;
            }

            @Override
            public void onAdFreeButtonClick(){}

            @Override
            public void onPurchaseRestored() {}
        });

        return processManager;
    }

    private boolean validateOxPush2Request(OxPush2Request oxPush2Request) {
        boolean result = true;
        try {
            boolean isOneStep = Utils.isEmpty(oxPush2Request.getUserName());
            boolean isTwoStep = Utils.areAllNotEmpty(oxPush2Request.getUserName(), oxPush2Request.getIssuer(), oxPush2Request.getApp(),
                    oxPush2Request.getState(), oxPush2Request.getMethod());
            boolean isCredManager = Utils.areAllNotEmpty(oxPush2Request.getUserName(), oxPush2Request.getIssuer(), oxPush2Request.getApp(), oxPush2Request.getMethod());

            if (BuildConfig.DEBUG) Log.d(TAG, "isOneStep: " + isOneStep + " isTwoStep: " + isTwoStep);

            if (isOneStep || isTwoStep) {
                // Valid authentication method should be used
                if (isTwoStep && !(Utils.equals(oxPush2Request.getMethod(), "authenticate") || Utils.equals(oxPush2Request.getMethod(), "enroll"))) {
                    result = false;
                }
            } else {
                // All fields must be not empty
                result = isCredManager;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to parse QR code");
            result = false;
        }

        if (!result) {
            Toast.makeText(getApplicationContext(), R.string.invalid_qr_code, Toast.LENGTH_LONG).show();
        }

        return result;
    }

    private void showAlertView(String title, String message){
        CustomAlert customAlert = new CustomAlert(this);
        customAlert.setHeader(title);
        customAlert.setMessage(message);
        customAlert.show();
    }

    private void showCameraMessagingAlertView() {
        CustomAlert customAlert = new CustomAlert(this);
        customAlert.setHeader(getString(R.string.camera_access));
        customAlert.setMessage(getString(R.string.camera_priming));
        customAlert.setCancelable(true);
        customAlert.setOnDismissListener(dialogInterface -> requestCameraPermission());
        customAlert.setOnCancelListener(dialogInterface -> requestCameraPermission());
        customAlert.show();
    }

    private void requestCameraPermission() {
        Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.CAMERA)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i(TAG,
                    "Displaying camera permission rationale to provide additional context.");
            ActivityCompat.requestPermissions(MainNavDrawerActivity.this,
                    new String[]{android.Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        } else {
            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }
    }

    public void selectDrawerItem(MenuItem menuItem) {
        reloadLogs();

        switch(menuItem.getItemId()) {
            case R.id.nav_keys:
                updateUIAfterNavItemSelected(new KeyFragmentListFragment());
                break;
            case R.id.nav_logs:
                updateUIAfterNavItemSelected(new LogsFragment());
                break;
            case R.id.nav_pin_code:
                updateUIAfterNavItemSelected(new SettingsPinCode());
                break;
            case R.id.nav_touch_id:
                updateUIAfterNavItemSelected(SettingsFragment.newInstance(SettingsFragment.Constant.FINGERPRINT_TYPE));
                break;
            case R.id.nav_ssl:
                updateUIAfterNavItemSelected(SettingsFragment.newInstance(SettingsFragment.Constant.SSL_CONNECTION_TYPE));
                break;
            case R.id.nav_user_guide:
                LicenseFragment termsFragment = LicenseFragment.newInstance(LicenseFragment.Type.TERMS_OF_SERVICE);
                updateUIAfterNavItemSelected(termsFragment);
                break;
            case R.id.nav_privacy_policy:
                LicenseFragment privacyFragment = LicenseFragment.newInstance(LicenseFragment.Type.PRIVACY);
                updateUIAfterNavItemSelected(privacyFragment);
                break;
            case R.id.nav_version:
                break;
        }

    }

    public void updateUIAfterNavItemSelected(Fragment fragment) {
        if(fragment != null) {
            fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        }

        drawer.closeDrawers();
    }

    public long getTimeDifferenceInSeconds() {
        Date currentDate = new Date();
        Date olderDate = new Date(Settings.getOxRequestTime(context));
        long timeDifferenceInMilliseconds = currentDate.getTime() - olderDate.getTime();
        return TimeUnit.SECONDS.convert(timeDifferenceInMilliseconds, TimeUnit.MILLISECONDS);
    }


    private void makeOxRequest(){
        String requestDataString = Settings.getOxRequestData(context);
        String userChoice = Settings.getUserChoice(context);

        if(userChoice == null || userChoice.equalsIgnoreCase(EntryActivity.NO_ACTION_PUSH)) {
            Intent intent = new Intent(MainNavDrawerActivity.QR_CODE_PUSH_NOTIFICATION);
            intent.putExtra(MainNavDrawerActivity.QR_CODE_PUSH_NOTIFICATION_MESSAGE, requestDataString);
            intent.putExtra(MainNavDrawerActivity.VIBRATE_AND_RINGTONE, false);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            final OxPush2Request oxPush2Request = new Gson().fromJson(requestDataString, OxPush2Request.class);
            final ProcessManager processManager = createProcessManager(oxPush2Request);
            Settings.clearPushOxData(context);
            if (userChoice.equalsIgnoreCase(EntryActivity.DENY_PUSH)) {
                showToastWithText(context.getString(R.string.process_deny_start));
                processManager.onOxPushRequest(true);
            } else if (userChoice.equalsIgnoreCase(EntryActivity.APPROVE_PUSH)) {
                showToastWithText(context.getString(R.string.process_authentication_start));
                processManager.onOxPushRequest(false);
            }
        }
    }

    private void showToastWithText(String text){
        CustomToast customToast = new CustomToast(context);
        LayoutInflater layoutInflater = getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.custom_toast, null);
        customToast.showGluuToastWithText(view, text);
    }
    //endregion

    //region inner class interfaces
    public interface GluuAlertCallback{
        void onPositiveButton();
        void onNegativeButton();
    }

    public interface RequestProcessListener{
        void onApprove();
        void onDeny();
    }

    //endregion

}
