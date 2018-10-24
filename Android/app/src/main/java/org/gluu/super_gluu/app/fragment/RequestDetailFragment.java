package org.gluu.super_gluu.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.codec.binary.StringUtils;
import org.gluu.super_gluu.app.LogState;
import org.gluu.super_gluu.app.NotificationType;
import org.gluu.super_gluu.app.activities.MainNavDrawerActivity;
import org.gluu.super_gluu.app.base.ToolbarFragment;
import org.gluu.super_gluu.app.customview.CustomAlert;
import org.gluu.super_gluu.app.model.LogInfo;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.store.AndroidKeyDataStore;
import org.gluu.super_gluu.util.Utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import SuperGluu.app.R;
import butterknife.BindView;
import butterknife.ButterKnife;

public class RequestDetailFragment extends ToolbarFragment {

    //region class variables
    SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private Boolean isUserInfo = false;
    private LogInfo logInfo;
    private OxPush2Request push2Request;
    private MainNavDrawerActivity.RequestProcessListener listener;
    public OnDeleteLogInfoListener deleteLogListener;

    private Timer clock;
    private Handler handler;

    private int seconds = 40;

    @BindView(R.id.nav_drawer_toolbar)
    Toolbar toolbar;

    @BindView(R.id.approve_layout)
    LinearLayout approveLayout;
    @BindView(R.id.deny_layout)
    LinearLayout denyLayout;

    @BindView(R.id.location_address_text_value)
    TextView addressTextView;
    @BindView(R.id.location_ip_text_value)
    TextView ipLocationTextView;
    @BindView(R.id.hour_minute_time_text_value)
    TextView hourMinuteTextView;
    @BindView(R.id.date_time_text_value)
    TextView dateTextView;
    @BindView(R.id.event_text_value)
    TextView eventTextView;
    @BindView(R.id.username_text_value)
    TextView usernameTextView;

    @BindView(R.id.application_text_view)
    TextView applicationTextView;

    @BindView(R.id.url_text_view)
    TextView urlTextView;

    @BindView(R.id.main_image_view)
    ImageView mainImageView;

    @BindView(R.id.timer_textView)
    TextView timerTextView;

    @BindView(R.id.action_button_group)
    LinearLayout approveDenyLayout;

    //endregion

    //region new instance and interfaces
    public static RequestDetailFragment newInstance(
            boolean isUserInfo, LogInfo logInfo, OxPush2Request oxPush2Request,
            MainNavDrawerActivity.RequestProcessListener listener) {

        RequestDetailFragment requestDetailFragment = new RequestDetailFragment();
        requestDetailFragment.setIsUserInfo(isUserInfo);
        requestDetailFragment.setLogInfo(logInfo);
        requestDetailFragment.setPush2Request(oxPush2Request);
        requestDetailFragment.setListener(listener);

        return requestDetailFragment;
    }

    public interface OnDeleteLogInfoListener {
        void onDeleteLogInfo(OxPush2Request oxPush2Request);
        void onDeleteLogInfo(List<LogInfo> logInfos);
    }
    //endregion

    //region lifecycle methods

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDeleteLogInfoListener) {
            deleteLogListener = (OnDeleteLogInfoListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDeleteKeyHandleListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_approve_deny, container, false);

        ButterKnife.bind(this, rootView);
        initHandler();

        //Setup fonts
        if (isUserInfo){
            setDefaultToolbar(toolbar, getString(R.string.log_details), true);

            timerTextView.setVisibility(View.GONE);
            approveDenyLayout.setVisibility(View.GONE);
        } else {
            setDefaultToolbar(toolbar, getString(R.string.permission_approval), false);

            startClockTick();
        }

        setHasOptionsMenu(true);

        updateLogInfo();
        updateLogo();
        approveLayout.setOnClickListener(view -> {
            if(listener != null) {
                listener.onApprove();
            }
            cleanUpViewAfterClick();
        });
        denyLayout.setOnClickListener(view -> {
            if(listener != null) {
                listener.onDeny();
            }
            cleanUpViewAfterClick();
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (seconds == 0){
            listener.onDeny();
            closeView();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(isUserInfo) {
            inflater.inflate(R.menu.menu_log_detail, menu);
        } else {
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.delete_action:
                showAlertView();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region helper methods
    private void updateLogInfo(){
        if (push2Request != null){

            URL url = null;
            try {
                url = new URL(push2Request.getIssuer());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if(url != null) {
                String baseUrl = getString(R.string.gluu_server)  + " " + url.getHost();
                applicationTextView.setText(baseUrl);
            }


            urlTextView.setText(push2Request.getIssuer());

            usernameTextView.setText(push2Request.getUserName());

            ipLocationTextView.setText(push2Request.getLocationIP());
            if (push2Request.getLocationCity() != null) {
                try {
                    addressTextView.setText(java.net.URLDecoder.decode(push2Request.getLocationCity(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    Log.d(this.getClass().getName(), e.getLocalizedMessage());
                }
            }
            AndroidKeyDataStore dataStore = new AndroidKeyDataStore(getContext());
            final List<byte[]> keyHandles = dataStore.getKeyHandlesByIssuerAndAppId(push2Request.getIssuer(), push2Request.getApp());
            final boolean isEnroll = (keyHandles.size() == 0) || StringUtils.equals(push2Request.getMethod(), "enroll");
            eventTextView.setText(capitalize(push2Request.getMethod()));
            hourMinuteTextView.setText(getTimeFromString(push2Request.getCreated()));
            dateTextView.setText(getDateFromString(push2Request.getCreated()));
        }
    }

    private void updateLogo(){

        if(isUserInfo && isFailedLog()) {
            mainImageView.setImageDrawable(getContext().getDrawable(R.drawable.log_detail_icon));
        } else {
            mainImageView.setImageDrawable(getContext().getDrawable(R.drawable.accept_deny_detail_icon));
        }
    }

    private String getDateFromString(String dateString){
        SimpleDateFormat userDateTimeFormat = new SimpleDateFormat("MMM d, yyyy");
        Date createdDate = null;
        if (Utils.isNotEmpty(dateString)) {
            if (isUserInfo){
                Date resultdate = new Date(Long.valueOf(dateString));
                return userDateTimeFormat.format(resultdate);
            } else {
                try {
                    createdDate = isoDateTimeFormat.parse(dateString);
                } catch (ParseException ex) {
                    Log.e(this.getClass().getName(), "Failed to parse ISO date/time: " + dateString, ex);
                }
            }
        }

        String createdString = "";
        if (createdDate != null) {
            createdString = userDateTimeFormat.format(createdDate);
        }
        return createdString;
    }

    private String getTimeFromString(String dateString){
        SimpleDateFormat userDateTimeFormat = new SimpleDateFormat("HH:mm:ss");
        Date createdDate = null;
        if (Utils.isNotEmpty(dateString)) {
            if (isUserInfo){
                Date resultdate = new Date(Long.valueOf(dateString));
                return userDateTimeFormat.format(resultdate);
            } else {
                try {
                    createdDate = isoDateTimeFormat.parse(dateString);
                } catch (ParseException ex) {
                    Log.e(this.getClass().getName(), "Failed to parse ISO date/time: " + dateString, ex);
                }
            }
        }

        String createdString = "";
        if (createdDate != null) {
            createdString = userDateTimeFormat.format(createdDate);
        }
        return createdString;
    }

    public static String capitalize(String text){
        String c = (text != null)? text.trim() : "";
        String[] words = c.split(" ");
        String result = "";
        for(String w : words){
            result += (w.length() > 1? w.substring(0, 1).toUpperCase(Locale.US) + w.substring(1, w.length()).toLowerCase(Locale.US) : w) + " ";
        }
        return result.trim();
    }

    void showAlertView(){
        MainNavDrawerActivity.GluuAlertCallback listener = new MainNavDrawerActivity.GluuAlertCallback(){
            @Override
            public void onPositiveButton() {
                if (deleteLogListener != null){
                    deleteLogListener.onDeleteLogInfo(push2Request);
                }
            }

            @Override
            public void onNegativeButton() {
                //Skip here
            }
        };
        CustomAlert customAlert = new CustomAlert(getActivity());
        customAlert.setHeader(getString(R.string.delete_log));
        customAlert.setMessage(getString(R.string.clear_log_message));
        customAlert.setPositiveText(getString(R.string.yes));
        customAlert.setNegativeText(getString(R.string.no));
        customAlert.setType(NotificationType.DELETE_LOG);
        customAlert.setListener(listener);
        customAlert.show();
    }

    public void cleanUpViewAfterClick() {
        stopClocking();
        closeView();
    }

    private void startClockTick(){
        clock = new Timer();
        clock.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                seconds--;
                //send message to update UI
                initHandler();

                handler.sendEmptyMessage(0);
            }
        }, 0, 1000);//put here time 1000 milliseconds=1 second
    }

    private void stopClocking(){
        if (clock != null) {
            clock.cancel();
        }
        clock = null;
    }

    private void closeView(){
        if (isUserInfo) {
            getActivity().invalidateOptionsMenu();
        }
        try {
            getFragmentManager().popBackStack();
        }
        catch (RuntimeException ex){
            //ignore there
        }
    }

    private void initHandler(){
        if(handler == null) {
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (seconds == 0){
                        stopClocking();
                        closeView();
                    }
                    String secondsText = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
                    timerTextView.setText(secondsText);
                }
            };
        }
    }

    public boolean isFailedLog() {
        return logInfo != null && (logInfo.getLogState() == LogState.ENROL_FAILED || logInfo.getLogState() == LogState.LOGIN_FAILED ||
                logInfo.getLogState() == LogState.UNKNOWN_ERROR || logInfo.getLogState() == LogState.LOGIN_DECLINED ||
                logInfo.getLogState() == LogState.ENROL_DECLINED);
    }

    //endregion

    //region getters and setters
    public void setIsUserInfo(Boolean isUserInfo) {
        this.isUserInfo = isUserInfo;
    }

    public Boolean getUserInfo() {
        return isUserInfo;
    }

    public void setLogInfo(LogInfo logInfo) {
        this.logInfo = logInfo;
    }

    public void setPush2Request(OxPush2Request push2Request) {
        this.push2Request = push2Request;
    }

    public MainNavDrawerActivity.RequestProcessListener getListener() {
        return listener;
    }

    public void setListener(MainNavDrawerActivity.RequestProcessListener listener) {
        this.listener = listener;
    }
    //endregion

}
