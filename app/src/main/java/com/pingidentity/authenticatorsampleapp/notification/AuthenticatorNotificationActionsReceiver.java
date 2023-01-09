package com.pingidentity.authenticatorsampleapp.notification;

import static com.pingidentity.authenticatorsampleapp.managers.NotificationsManager.AUTHENTICATOR_NOTIFICATION_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.pingidentity.pingidsdkv2.NotificationObject;

public class AuthenticatorNotificationActionsReceiver extends BroadcastReceiver {

    public static final String ACTION_APPROVE = "com.pingidentity.authenticatorsampleapp.APPROVE";
    public static final String ACTION_DENY = "com.pingidentity.authenticatorsampleapp.DENY";

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {

        NotificationObject notificationObject = intent.getBundleExtra("extra").getParcelable("NotificationObject");

        if (intent.getAction()!=null && intent.getAction().equalsIgnoreCase(ACTION_APPROVE)){
            notificationObject.approve(context, "user_notification_approval", pingOneSDKError ->
                    Log.i("AuthenticatorNotificationActionsReceiver", "Approve action completed successfully"));
        }
        if (intent.getAction()!=null && intent.getAction().equalsIgnoreCase(ACTION_DENY)){
            notificationObject.deny(context, pingOneSDKError ->
                    Log.i("AuthenticatorNotificationActionsReceiver", "Deny action completed successfully"));
        }
        /*
         * cancel the notification on action received
         */
        NotificationManagerCompat.from(context).cancel(AUTHENTICATOR_NOTIFICATION_ID);
    }

}
