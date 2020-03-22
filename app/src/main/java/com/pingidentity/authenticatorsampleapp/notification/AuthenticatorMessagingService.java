package com.pingidentity.authenticatorsampleapp.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.navigation.NavDeepLinkBuilder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pingidentity.authenticatorsampleapp.AuthenticationActivity;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.NotificationsManager;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

public class AuthenticatorMessagingService extends FirebaseMessagingService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PingOne.setDeviceToken(this, token, new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
            }
        });
    }

    @Override
    public void onMessageReceived(final @NonNull RemoteMessage remoteMessage) {
        PingOne.processRemoteNotification(remoteMessage, new PingOne.PingOneNotificationCallback() {
            @Override
            public void onComplete(@Nullable NotificationObject notificationObject, @Nullable PingOneSDKError pingOneSDKError) {
                if(notificationObject!=null){
                    if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                        Intent intent = new Intent(AuthenticatorMessagingService.this, AuthenticationActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Bundle data = new Bundle();
                        data.putParcelable("PingOneNotificationObject", notificationObject);
                        intent.putExtras(data);
                        startActivity(intent);
                    }else{
                        NotificationsManager notificationsManager = new NotificationsManager(AuthenticatorMessagingService.this);
                        notificationsManager.buildAndSendPendingIntentNotification(notificationObject);
                    }
                }
            }
        });
    }
}
