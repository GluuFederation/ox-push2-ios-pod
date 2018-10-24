package org.gluu.super_gluu.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import org.gluu.super_gluu.app.GluuApplication;
import org.gluu.super_gluu.app.LogState;
import org.gluu.super_gluu.app.activities.EntryActivity;
import org.gluu.super_gluu.app.activities.MainNavDrawerActivity;
import org.gluu.super_gluu.app.model.LogInfo;
import org.gluu.super_gluu.model.OxPush2Request;
import org.gluu.super_gluu.u2f.v2.model.TokenEntry;

import java.util.ArrayList;
import java.util.List;

import SuperGluu.app.R;

/**
 * Created by SamIAm on 3/14/18.
 */

public abstract class FakeDataUtil {

    public static final String FAKE_LICENSED_JSON = "{\"app\":\"https://cred3.gluu.org/cred-manager\",\"method\":\"enroll\",\"req_ip\":\"38.142.29.4\",\"created\":\"2018-06-04T16:38:42.615\",\"req_loc\":\"United States, Texas, Houston\",\" \",\"username\":\"eric4\",\"enrollment\":\"e461af7a-c97e-4820-89d4-c30da5ad6ca1\", \"licensed\": true}";


    private static final String QR_CODE_PUSH_NOTIFICATION_MESSAGE =
            MainNavDrawerActivity.class.getPackage().getName() + ".QR_CODE_PUSH_NOTIFICATION_MESSAGE";
    private static final String QR_CODE_PUSH_NOTIFICATION = "QR_CODE_PUSH_NOTIFICATION";

    public static Intent getFakeAuthBroadcastIntent() {
        String message =
                "{\"app\":\"https://cred3.gluu.org/cred-manager\",\"method\":\"authenticate\",\"req_ip\":\"38.142.29.4\",\"created\":\"2018-03-14T19:41:29.094000\",\"issuer\":\"https://cred3.gluu.org\",\"req_loc\":\"United%20States%2C%20Texas%2C%20Houston\",\"state\":\"00f14ff3-e153-4f1f-a4c4-4587241b3b4d\",\"username\":\"eric3\"}\n";

        Intent intent = new Intent(QR_CODE_PUSH_NOTIFICATION);
        intent.putExtra(QR_CODE_PUSH_NOTIFICATION_MESSAGE, message);
        return intent;
    }

    public static OxPush2Request getFakeOXRequest() {
        OxPush2Request oxPush2Request =
                new OxPush2Request("eric3",
                        "https://cred3.gluu.org",
                        "https://cred3.gluu.org/cred-manager",
                        "00f14ff3-e153-4f1f-a4c4-4587241b3b4d",
                        "authenticate", "2018-03-14T19:41:29.094000",
                        null, true);
        oxPush2Request.setLocationCity("United%20States%2C%20Texas%2C%20Houston");
        oxPush2Request.setLocationIP("38.142.29.4");
        return oxPush2Request;
    }

    public static String getFakePushJsonString() {
        return "{\"app\":\"https://cred3.gluu.org/cred-manager\",\"method\":\"authenticate\",\"req_ip\":\"72.48.183.102\",\"created\":\"2018-04-12T17:47:20.838000\",\"issuer\":\"https://cred3.gluu.org\",\"req_loc\":\"United%20States%2C%20Texas%2C%20Austin\",\"state\":\"9d7b1684-b3ab-415a-9459-d2c878bb5c55\",\"username\":\"eric2\"}";
    }


    public static void sendNotification(Context context) {

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String contentText = "Example Content Text";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, GluuApplication.CHANNEL_ID)
                .setContentIntent(getFakePendingIntent(context))
                .setSmallIcon(R.drawable.push_icon)
                .setSound(defaultSoundUri)
                .setContentTitle("Example Title")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText))
                .setChannelId(GluuApplication.CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setVibrate(new long[]{ 100, 250, 100, 250, 100, 250})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.deny_icon_push, "Deny", getFakePendingIntent(context))
                .addAction(R.drawable.approve_icon_push, "Approve", getFakePendingIntent(context));

        if(notificationManager != null) {
            notificationManager.notify(MainNavDrawerActivity.MESSAGE_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private static PendingIntent getFakePendingIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, EntryActivity.class);
        return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static List<TokenEntry> getFakeListOfTokens(int size) {

        ArrayList<TokenEntry> tokenEntries = new ArrayList<>();

        for(int i = 0; i < size; i++) {
            tokenEntries.add(getFakeTokenEntry());
        }

        return tokenEntries;
    }

    private static TokenEntry getFakeTokenEntry() {
        TokenEntry tokenEntry = new TokenEntry();

        tokenEntry.setApplication("https://cred3.gluu.org/cred-manager");
        tokenEntry.setKeyPair("{\"privateKey\":{\"d\":\"00ad1e966c4cb3bb806d591a36c17fdc4084aff20c312818dc0ab7ce8590c2881a\"},\"publicKey\":{\"x\":\"00cb930c97d36afa3ba889c8ff4b3c443a9f4e7d527c7733978985c49589310730\",\"y\":\"5f762175925130cd66e2d4e2247434fc3fc35dd044277f1600b7938700cbf69d\"}}");
        tokenEntry.setUserName("eric3");
        tokenEntry.setAuthenticationMode("enroll");
        tokenEntry.setAuthenticationType("TwoStep");
        tokenEntry.setKeyHandle(new byte[50]);
        tokenEntry.setKeyName("https://cred3.gluu.org");
        tokenEntry.setIssuer("https://cred3.gluu.org");
        tokenEntry.setCreatedDate("2018-03-07T16:16:47.626");

        return tokenEntry;
    }

    public static List<LogInfo> getFakeListOfLogs(int size) {
        ArrayList<LogInfo> logInfo = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            logInfo.add(getFakeLogInfo());
        }

        return logInfo;
    }

    private static LogInfo getFakeLogInfo() {
        LogInfo logInfo = new LogInfo();

        logInfo.setLocationAddress("United States, Texas, Houston");
        logInfo.setLocationIP("38.142.29.4");
        logInfo.setLogState(LogState.ENROL_SUCCESS);
        logInfo.setMessage(null);
        logInfo.setCreatedDate("1520958064534");
        logInfo.setUserName("eric3");
        logInfo.setIssuer("https://cred3.gluu.org");
        logInfo.setMethod("enroll");

        return logInfo;
    }
}
