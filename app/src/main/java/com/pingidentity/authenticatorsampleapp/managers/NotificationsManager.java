package com.pingidentity.authenticatorsampleapp.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.util.Pair;

import com.pingidentity.authenticatorsampleapp.AuthenticationActivity;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.pingidsdkv2.NotificationObject;

public class NotificationsManager {


    private static final String CHANNEL_ID = "PingOneNotificationsChannel";
    private Context context;

    public NotificationsManager(Context context){
        this.context = context;
        createNotificationChannel();
    }

    /**
     * Before you can deliver the notification on Android 8.0 and higher,
     * you must register your app's notification channel with the system
     * by passing an instance of NotificationChannel to createNotificationChannel().
     * So the following code is blocked by a condition on the SDK_INT version
     */
    private void createNotificationChannel() {
        /*
         * Create the NotificationChannel, but only on API 26+
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notifications_channel_name);
            String description = context.getString(R.string.notifications_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private PendingIntent createOnTapPendingIntent(NotificationObject notificationObject, Pair<String, String> content){
        Intent intent = new Intent(context, AuthenticationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Bundle data = new Bundle();
        data.putParcelable("PingOneNotificationObject", notificationObject);
        intent.putExtras(data);
        intent.putExtra("title", content.first);
        intent.putExtra("body", content.second);
        return PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void buildAndSendPendingIntentNotification(NotificationObject notificationObject, Pair<String, String> content){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(content.first)
                .setContentText(content.second)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(createOnTapPendingIntent(notificationObject, content))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(12345, builder.build());
    }
}
