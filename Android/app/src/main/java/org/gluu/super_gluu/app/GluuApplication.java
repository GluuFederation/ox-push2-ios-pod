package org.gluu.super_gluu.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import SuperGluu.app.R;

/**
 * Created by nazaryavornytskyy on 5/9/16.
 */
public class GluuApplication extends MultiDexApplication implements LifecycleObserver {

    private static GluuApplication sInstance;

    public static String CHANNEL_ID = "super_gluu_default_channel";

    private static boolean isAppInForeground;
    private static boolean wentThroughLauncherActivity = false;
    public static boolean isTrustAllCertificates = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        createDefaultNotificationChannel();
    }

    public static boolean isIsAppInForeground() {
        return isAppInForeground;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onAppForegrounded() {
        isAppInForeground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onAppBackgrounded() {
        isAppInForeground = false;
        wentThroughLauncherActivity = false;
    }

    public GluuApplication() {
        sInstance = this;
    }

    public static GluuApplication get() {
        return sInstance;
    }

    public static void setWentThroughLauncherActivity(boolean wentThroughLauncherActivity) {
        GluuApplication.wentThroughLauncherActivity = wentThroughLauncherActivity;
    }

    public static boolean didAppGoThroughLauncherActivity() {
        return wentThroughLauncherActivity;
    }

    public void createDefaultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);

            if(notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
