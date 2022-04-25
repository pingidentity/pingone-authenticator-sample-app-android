package com.pingidentity.authenticatorsampleapp.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.util.Pair;

import com.pingidentity.authenticatorsampleapp.AuthenticationActivity;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.pingidsdkv2.NotificationObject;

public class NotificationsManager {


    private static final String CHANNEL_ID = "PingOneNotificationsChannel";
    private final Context context;

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

    private PendingIntent createOnTapPendingIntent(NotificationObject notificationObject, Pair<String, String> content){
        Intent intent = new Intent(context, AuthenticationActivity.class);
        intent.setFlags(
                /*
                 * When using this flag, if a task is already running for the activity you are now
                 * starting, then a new activity will not be started; instead, the current task will
                 * simply be brought to the front of the screen with the state it was last in
                 */
                Intent.FLAG_ACTIVITY_NEW_TASK |
                /*
                 * This can only be used in conjunction with FLAG_ACTIVITY_NEW_TASK.
                 * This flag will cause any existing task that would be associated with the activity
                 * to be cleared before the activity is started
                 */
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                /*
                 * This flag will bring any currently running instance of that task to the
                 * foreground, and then clear it to its root state. This is especially useful,
                 * for example, when launching an activity from the notification manager
                 */
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle data = new Bundle();
        data.putParcelable("PingOneNotificationObject", notificationObject);
        intent.putExtras(data);
        intent.putExtra("title", content.first);
        intent.putExtra("body", content.second);
        return PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), intent, PendingIntent.FLAG_UPDATE_CURRENT |PendingIntent.FLAG_MUTABLE);
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
