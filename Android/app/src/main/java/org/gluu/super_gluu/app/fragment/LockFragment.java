package org.gluu.super_gluu.app.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gluu.super_gluu.app.customview.CustomAlert;
import org.gluu.super_gluu.app.settings.Settings;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import SuperGluu.app.R;

/**
 * Created by nazaryavornytskyy on 4/20/16.
 */
public class LockFragment extends Fragment {

    private Timer clock;
    private Handler handler;
    private OnLockAppTimerOver listener;

    private Context context;

    private LinearLayout timerView;
    private TextView minutesTextView;
    private TextView secondsTextView;

    long minutes = Settings.Constant.APP_LOCKED_MINUTES;
    long seconds = 0;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLockAppTimerOver) {
            listener = (OnLockAppTimerOver) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLockAppTimerOver");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        context = getContext();
        View rootView = inflater.inflate(R.layout.fragment_lock_app, container, false);

        timerView = rootView.findViewById(R.id.timer_view);

        minutesTextView = rootView.findViewById(R.id.locked_screen_minutes);
        secondsTextView = rootView.findViewById(R.id.locked_screen_seconds);

        if(hasTimeExpired()) {
            timerView.setVisibility(View.GONE);
            onTimeHasExpired();
        } else {
            timerView.setVisibility(View.VISIBLE);
            calculateTimeLeft();
            initHandler();
            startClockTick();
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(hasTimeExpired()) {
            timerView.setVisibility(View.GONE);
            onTimeHasExpired();
        }
    }

    private void initHandler(){
        if(handler == null) {
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (minutes <= 0 && seconds <= 0){
                        onTimeHasExpired();
                    }
                    String minStr = minutes < 10 ? "0" + minutes + ":" : minutes + ":";
                    String secStr = seconds < 10 ? "0" + seconds : String.valueOf(seconds);
                    minutesTextView.setText(minStr);
                    secondsTextView.setText(secStr);
                }
            };
        }
    }

    private void startClockTick() {
        if (hasTimeExpired()) {
            onTimeHasExpired();
        } else {
            showAlertView(getString(R.string.wrong_code), getString(R.string.entered_wrong_pin_code_limit_message));
            clock = new Timer();
            clock.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (seconds == 0) {
                        minutes = minutes - 1;
                        minutes = minutes < 0 ? 0 : minutes;
                        seconds = 60;
                    }
                    seconds--;
                    //send message to update UI
                    handler.sendEmptyMessage(0);
                }
            }, 0, 1000);//put here time 1000 milliseconds=1 second
        }
    }

    private void stopClocking(){
        if (clock != null) {
            clock.cancel();
            clock = null;
        }
    }

    private void onTimeHasExpired() {
        stopClocking();
        Settings.setAppLocked(context, false);
        Settings.clearAppLockedTime(context);
        Settings.resetCurrentPinAttempts(context);
        listener.onTimerOver();
    }


    private boolean hasTimeExpired() {
        Date currentTime = new Date(System.currentTimeMillis());
        return currentTime.getTime() >= Settings.getAppLockedTime(context);
    }

    private void calculateTimeLeft() {
        Date currentTime = new Date(System.currentTimeMillis());

        long difference = Settings.getAppLockedTime(context) - currentTime.getTime()  ;

        long minutesDifference = TimeUnit.MILLISECONDS.toMinutes(difference);
        long secondsDifference = TimeUnit.MILLISECONDS.toSeconds(difference);

        minutes = minutesDifference;
        seconds = secondsDifference - (minutesDifference * 60);
    }


    private void showAlertView(String title, String message){
        CustomAlert gluuAlert = new CustomAlert(getActivity());
        gluuAlert.setMessage(message);
        gluuAlert.setHeader(title);
        gluuAlert.show();
    }

    public interface OnLockAppTimerOver {
        void onTimerOver();
    }
}
