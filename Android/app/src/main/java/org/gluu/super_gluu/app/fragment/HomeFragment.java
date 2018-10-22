/*
 *  oxPush2 is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 *  Copyright (c) 2014, Gluu
 */

package org.gluu.super_gluu.app.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.gluu.super_gluu.app.ProcessManager;
import org.gluu.super_gluu.app.activities.CustomBarcodeScannerActivity;
import org.gluu.super_gluu.app.activities.MainNavDrawerActivity;
import org.gluu.super_gluu.app.customview.CustomAlert;
import org.gluu.super_gluu.app.customview.CustomToast;
import org.gluu.super_gluu.app.listener.OxPush2RequestListener;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.store.AndroidKeyDataStore;
import org.gluu.super_gluu.u2f.v2.SoftwareDevice;
import org.gluu.super_gluu.u2f.v2.exception.U2FException;
import org.gluu.super_gluu.u2f.v2.model.TokenResponse;
import org.gluu.super_gluu.u2f.v2.store.DataStore;
import org.json.JSONException;

import java.io.IOException;

import SuperGluu.app.BuildConfig;
import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Main activity fragment
 *
 * Created by Yuriy Movchan on 12/28/2015.
 */
public class HomeFragment extends Fragment implements TextView.OnEditorActionListener {

    private static final String TAG = "main-activity-fragment";

    private OxPush2RequestListener oxPush2RequestListener;

    private Context context;

    @BindView(R.id.button_scan)
    Button scanButton;

    @BindView(R.id.adView)
    AdView adView;

    @BindView(R.id.remove_ad_card_view)
    CardView removeAdView;

    @BindView(R.id.button_remove_ads)
    Button removeAdsButton;

    @BindView(R.id.welcome_text_view)
    TextView welcomeTextView;
    @BindView(R.id.description_text_view)
    TextView descriptionTextView;

    private SoftwareDevice u2f;
    private AndroidKeyDataStore dataStore;

    public interface GluuAdListener {
        void showInterstitialAd();
        boolean areAdsDisabled();
    }

    GluuAdListener gluuAdListener;

    private BroadcastReceiver adBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean isAdFree = intent.getBooleanExtra("isAdFree", false);

            if (context != null) {
                handleAdBroadcastIntent(isAdFree);
            }
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int messageId = intent.getIntExtra("message", 0);
            if (context != null && messageId != 0) {
                showDialog(messageId);
            }
        }
    };

    private BroadcastReceiver onAdFree = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            runSubscribeFlow();
        }
    };

    private BroadcastReceiver onRestorePurchase = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            runRestorePurchaseFlow();
        }
    };

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ButterKnife.bind(this, view);

        context = view.getContext();
        this.dataStore = new AndroidKeyDataStore(context);
        this.u2f = new SoftwareDevice(getActivity(), dataStore);

        scanButton.setOnClickListener(scanView -> submit());
        removeAdsButton.setOnClickListener(buttonView -> runSubscribeFlow());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onAdFree,
                new IntentFilter("on-ad-free-flow"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(onRestorePurchase,
                new IntentFilter("on-restore-purchase-flow"));

        setupBannerAd();

        return view;
    }

    private void setupBannerAd() {

        if(gluuAdListener.areAdsDisabled()) {
            adView.setVisibility(View.GONE);
            removeAdView.setVisibility(View.GONE);
        } else {
            MobileAds.initialize(getActivity().getApplicationContext(), BuildConfig.BANNER_AD_ID);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isConnected(context)) {
            scanButton.setEnabled(false);
        } else {
            scanButton.setEnabled(true);
        }
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("ox_request-precess-event"));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(adBroadcastReceiver,
                new IntentFilter("on-ad-free-event"));

        if(adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onPause() {
        if(adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(adBroadcastReceiver);
    }

    @Override
    public void onDestroy() {
        if(adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OxPush2RequestListener) {
            oxPush2RequestListener = (OxPush2RequestListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }

        if (context instanceof GluuAdListener) {
            gluuAdListener = (GluuAdListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement GluuAdListener");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    // Parsing bar code reader result
                    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Parsing QR code result: " + result.toString());
                    OxPush2Request oxPush2Request;
                    try {
                        oxPush2Request = new Gson().fromJson(result.getContents(), OxPush2Request.class);
                    }
                    catch (Exception ex){
                        oxPush2Request = null;
                    }
                    onQrRequest(oxPush2Request);
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    showToastWithText(context.getString(R.string.canceled_process_qr_code));
                }
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            submit();
            return true;
        }

        return false;
    }

    private void runSubscribeFlow(){
        if (oxPush2RequestListener != null) {
            oxPush2RequestListener.onAdFreeButtonClick();
        }
    }

    private void runRestorePurchaseFlow(){
        if (oxPush2RequestListener != null) {
            oxPush2RequestListener.onPurchaseRestored();
        }
    }

    private void onQrRequest(OxPush2Request oxPush2Request){
        if (oxPush2Request == null){
            showToastWithText("You scanned wrong QR code");
        } else {
            oxPush2RequestListener.onQrRequest(oxPush2Request);
        }
    }

    private void showToastWithText(String text){
        CustomToast customToast = new CustomToast(context);
        LayoutInflater layoutInflater = getLayoutInflater();
        if(layoutInflater != null) {
            View view = getLayoutInflater().inflate(R.layout.custom_toast, null);
            customToast.showGluuToastWithText(view, text);
        }
    }

    private void submit() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setCaptureActivity(CustomBarcodeScannerActivity.class);
        integrator.initiateScan();
    }


    private void showDialog(int messageId){
        Activity activity = getActivity();
        Pair<String, String> titleMessageText = getTitleBasedOnMessage(messageId);

        final CustomAlert gluuAlert = new CustomAlert(activity);
        gluuAlert.setHeader(titleMessageText.first);
        gluuAlert.setMessage(titleMessageText.second);
        gluuAlert.setPositiveText(getString(R.string.ok));
        gluuAlert.setOnCancelListener(dialogInterface -> showInterstitialAd());
        gluuAlert.setListener(new MainNavDrawerActivity.GluuAlertCallback() {
            @Override
            public void onPositiveButton() {
                showInterstitialAd();
            }

            @Override
            public void onNegativeButton() {
            }
        });
        gluuAlert.show();
    }

    public void showInterstitialAd() {

        if(!gluuAdListener.areAdsDisabled()) {
            gluuAdListener.showInterstitialAd();
        }
    }


    private Pair<String, String> getTitleBasedOnMessage(int messageId) {

        switch (messageId) {
            case Constant.AUTH_SUCCESS:
                return new Pair<>(getString(R.string.success), getString(R.string.auth_result_success));
            case Constant.AUTH_FAILURE:
            case Constant.AUTHENTICATED_FAILED_OTHER:
                return new Pair<>(getString(R.string.failed), getString(R.string.deny_result_success));
            case Constant.ENROLLMENT_SUCCESS:
                return new Pair<>(getString(R.string.enroll_result_title), getString(messageId));
            case Constant.FIDO_U2F_INVALID:
                return new Pair<>(getString(R.string.fido_failure), getString(messageId));
            case Constant.CHALLENGE:
                return new Pair<>(getString(R.string.challenge), getString(messageId));
            case Constant.DECLINE_FAILED:
                return new Pair<>(getString(R.string.decline), getString(messageId));
            case Constant.DUPLICATE_ENROLLMENT:
                return new Pair<>(getString(messageId), getString(R.string.existing_key_message));
            default:
                return new Pair<>(getString(R.string.generic_auth_result), getString(messageId));
        }
    }

    private ProcessManager createProcessManager(OxPush2Request oxPush2Request){
        ProcessManager processManager = new ProcessManager();
        processManager.setOxPush2Request(oxPush2Request);
        processManager.setDataStore(dataStore);
        processManager.setActivity(getActivity());
        processManager.setOxPush2RequestListener(new OxPush2RequestListener() {
            @Override
            public void onQrRequest(OxPush2Request oxPush2Request) {
                //skip code there
            }

            @Override
            public TokenResponse onSign(String jsonRequest, String origin, Boolean isDeny) throws JSONException, IOException, U2FException {
                return u2f.sign(jsonRequest, origin, isDeny);
            }

            @Override
            public TokenResponse onEnroll(String jsonRequest, OxPush2Request oxPush2Request, Boolean isDeny) throws JSONException, IOException, U2FException {
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

    private static boolean isConnected(final Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }

        return false;
    }

    private void handleAdBroadcastIntent(Boolean isAdFree){
        if (isAdFree) {
            removeAdView.setVisibility(View.GONE);
            adView.setVisibility(View.GONE);
        }
    }

    private class Constant {
        private static final int AUTH_SUCCESS = R.string.auth_result_success;
        private static final int AUTH_FAILURE = R.string.deny_result_success;
        private static final int ENROLLMENT_SUCCESS = R.string.enroll_result_success;
        private static final int FIDO_U2F_INVALID = R.string.wrong_token_response;
        private static final int CHALLENGE = R.string.challenge_doesnt_match;
        private static final int DECLINE_FAILED = R.string.deny_result_failed;
        private static final int AUTHENTICATED_FAILED_OTHER = R.string.auth_result_failed;
        private static final int DUPLICATE_ENROLLMENT = R.string.duplicate_enrollment_title;
        private static final int FAILED_PROCESS_STATUS = R.string.failed_process_status;
        private static final int FAILED_PROCESS_RESPONSE = R.string.failed_process_response;
        private static final int NO_VALID_KEY_HANDLES = R.string.no_valid_key_handles;
        private static final int FAILED_PROCESS_CHALLENGE = R.string.failed_process_challenge;
        private static final int WRONG_U2F_METADATA = R.string.wrong_u2f_metadata;
    }

}
