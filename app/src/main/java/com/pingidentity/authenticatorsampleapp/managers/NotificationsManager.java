package com.pingidentity.authenticatorsampleapp.managers;

import static com.pingidentity.authenticatorsampleapp.notification.AuthenticatorNotificationActionsReceiver.ACTION_APPROVE;
import static com.pingidentity.authenticatorsampleapp.notification.AuthenticatorNotificationActionsReceiver.ACTION_DENY;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.pingidentity.authenticatorsampleapp.MainActivity;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.notification.AuthenticatorNotificationActionsReceiver;

public class NotificationsManager {


    public static final int AUTHENTICATOR_NOTIFICATION_ID = 12345;
    private static final String CHANNEL_ID = "PingOneNotificationsChannel";
    private final Context context;

    public NotificationsManager(Context context){
        this.context = context;
        createNotificationChannel();
    }

    /*
     * Before you can deliver the notification you must register your app's notification channel
     * with the system by passing an instance of NotificationChannel to createNotificationChannel().
     * It's safe to call this repeatedly because creating an existing notification channel performs
     * no operation.
     */
    private void createNotificationChannel() {

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

    /*
     * there are several possible types of categories in remoteMessage data, build a
     * notification accordingly:
     * 1. category is null: build a notification without action buttons in analogy with iOS
     * 2. category "auth": build a notification with approve/deny buttons
     * 3. category "auth_open": build a notification with approve/deny buttons where approve action
     * will also open the application
     */
    public void buildAndSendNotificationAccordingToCategory(@NonNull Intent notificationIntent){
        String category = notificationIntent.getStringExtra("category");
        Log.i(NotificationsManager.class.getCanonicalName(),
                "build notification for category " + category);
        if (category==null){
            buildAndSendPlainNotification(notificationIntent, true);
            return;
        }
        switch (category){
            case ("auth"):
                buildAndSendActionsNotification(notificationIntent, false);
                break;
            case ("auth_open"):
                buildAndSendActionsNotification(notificationIntent, true);
                break;
            default:
                buildAndSendPlainNotification(notificationIntent, true);
                break;
        }
    }

    public void buildAndSendPlainNotification(Intent notificationIntent, boolean openOnTap){
        NotificationCompat.Builder builder = prepareNotification(notificationIntent);
        if (openOnTap){
            builder.setContentIntent(createOnTapPendingIntent(notificationIntent));
        }
        NotificationManagerCompat.from(context).notify(AUTHENTICATOR_NOTIFICATION_ID, builder.build());
    }

    private void buildAndSendActionsNotification(Intent notificationIntent, boolean openOnApprove) {

        NotificationCompat.Builder builder = prepareNotification(notificationIntent);

        builder.setContentIntent(createOnTapPendingIntent(notificationIntent));

        Bundle bundle = new Bundle();
        bundle.putParcelable("NotificationObject", notificationIntent.getParcelableExtra("PingOneNotificationObject"));
        if (openOnApprove){
            builder.addAction(createApproveAndOpenAction(bundle));
        }else {
            builder.addAction(createApproveAction(bundle));
        }
        builder.addAction(createDenyAction(bundle));

        NotificationManagerCompat.from(context).notify(AUTHENTICATOR_NOTIFICATION_ID, builder.build());
    }




    @NonNull
    private NotificationCompat.Builder prepareNotification(@NonNull Intent notificationIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        if(notificationIntent.hasExtra("title")) {
            builder.setContentTitle(notificationIntent.getStringExtra("title"));
        }
        if (notificationIntent.hasExtra("body")){
            builder.setContentText(notificationIntent.getStringExtra("body"));
        }
        return builder;
    }

    @NonNull
    private NotificationCompat.Action createApproveAction(Bundle bundle){
        Intent approveIntent = new Intent(context, AuthenticatorNotificationActionsReceiver.class);
        approveIntent.setAction(ACTION_APPROVE);
        approveIntent.putExtra("extra", bundle);
        PendingIntent approvePendingIntent = PendingIntent.getBroadcast(context, 1,
                approveIntent, PendingIntent.FLAG_UPDATE_CURRENT |PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action.Builder(
                0,
                context.getString(R.string.notification_action_approve), approvePendingIntent)
                .build();
    }

    @NonNull
    private NotificationCompat.Action createDenyAction(Bundle bundle){
        Intent denyIntent = new Intent(context, AuthenticatorNotificationActionsReceiver.class);
        denyIntent.setAction(ACTION_DENY);
        denyIntent.putExtra("extra", bundle);
        PendingIntent denyPendingIntent = PendingIntent.getBroadcast(context, 2,
                denyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action.Builder(
                0,
                context.getString(R.string.notification_action_deny), denyPendingIntent)
                .build();
    }

    /*
     * Since Android 10 it is restricted to start Activity from BroadcastReceiver.
     * Thus, to achieve expected functionality we create a PendingIntent to an Activity instead
     * of Broadcast.
     */
    @NonNull
    private NotificationCompat.Action createApproveAndOpenAction(Bundle data){
        Intent approveAndOpenIntent = new Intent(context, MainActivity.class);
        approveAndOpenIntent.putExtras(data);
        PendingIntent approveAndOpenPendingIntent =  PendingIntent.getActivity(context,
                (int) (System.currentTimeMillis() & 0xfffffff), approveAndOpenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action.Builder(
                0,
                context.getString(R.string.notification_action_approve), approveAndOpenPendingIntent)
                .build();
    }

    /*
     * convert a notificationObject Intent to PendingIntent that will start a new Activity
     */
    private PendingIntent createOnTapPendingIntent(@NonNull Intent handleNotificationObjectIntent){
        return PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                handleNotificationObjectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }
}
