package org.gluu.super_gluu.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.gluu.super_gluu.app.GluuApplication;
import org.gluu.super_gluu.app.activities.EntryActivity;
import org.gluu.super_gluu.app.activities.MainNavDrawerActivity;
import org.gluu.super_gluu.app.settings.Settings;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.util.Utils;

import SuperGluu.app.R;

/**
 * Created by nazaryavornytskyy on 4/3/17.
 */
public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FBMessagingService";
    public static final String DENY_ACTION = "DENY_ACTION";
    public static final String APPROVE_ACTION = "APPROVE_ACTION";
    public static final String PUSH_NO_ACTION = "PUSH_ACTION";

    public static final int APPROVE_TYPE = 10;
    public static final int DENY_TYPE = 20;
    public static final int NO_ACTION_TYPE = 30;


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
// example message json: "app":"https://ce-release.gluu.org/identity/authentication/authcode", "username": "admin", "method": "enroll", "state": "35e37974-db3d-474b-ba4e-35fda6499ba9", "created": "2016-04-11T09:31:01.020000", "issuer": "https://ce-release.gluu.org"
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
//            String requestMessage = remoteMessage.getData().get("request");
            if (Utils.isEmpty(title) || Utils.isEmpty(message)) {
                Log.e(TAG, "Get unknown push notification message: " + remoteMessage.getData().toString());
                return;
            }

            Settings.setPushOxRequestTime(getApplicationContext());
            Settings.setPushOxData(getApplicationContext(), message);

            if (GluuApplication.isIsAppInForeground()) {
                Intent intent = new Intent(MainNavDrawerActivity.QR_CODE_PUSH_NOTIFICATION);
                intent.putExtra(MainNavDrawerActivity.QR_CODE_PUSH_NOTIFICATION_MESSAGE, message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            } else {
                sendNotification(getString(R.string.push_title), message);
            }

        }
    }

    private void sendNotification(String title, String message) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        PendingIntent denyIntent = createPendingIntent(DENY_TYPE);
        PendingIntent approveIntent = createPendingIntent(APPROVE_TYPE);
        PendingIntent mainPendingIntent = createPendingIntent(NO_ACTION_TYPE);

        String contentText = getContentText(message);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, GluuApplication.CHANNEL_ID)
                .setContentIntent(mainPendingIntent)
                .setSmallIcon(R.drawable.push_icon)
                .setSound(defaultSoundUri)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText))
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setChannelId(GluuApplication.CHANNEL_ID)
                .setVibrate(new long[]{ 100, 250, 100, 250, 100, 250})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.deny_icon_push, getString(R.string.deny), denyIntent)
                .addAction(R.drawable.approve_icon_push, getString(R.string.approve), approveIntent);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(notificationManager != null) {
            notificationManager.notify(MainNavDrawerActivity.MESSAGE_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private String getContentText(String message) {
        OxPush2Request push2Request = new Gson().fromJson(message, OxPush2Request.class);

        if(push2Request == null || push2Request.getApp() == null) {
            return "";
        } else {
            return push2Request.toPushMessage(getString(R.string.push_login_format));
        }

    }

    private PendingIntent createPendingIntent(int type){
        Intent intent = new Intent(this, EntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        switch (type) {
            case DENY_TYPE:
                intent.setAction(DENY_ACTION);
                break;
            case APPROVE_TYPE:
                intent.setAction(APPROVE_ACTION);
                break;
            case NO_ACTION_TYPE:
                intent.setAction(PUSH_NO_ACTION);
                break;
        }

        return PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

}
