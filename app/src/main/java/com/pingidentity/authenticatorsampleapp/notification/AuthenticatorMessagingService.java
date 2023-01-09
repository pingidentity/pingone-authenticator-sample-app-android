package com.pingidentity.authenticatorsampleapp.notification;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pingidentity.authenticatorsampleapp.AuthenticationActivity;
import com.pingidentity.authenticatorsampleapp.managers.NotificationsManager;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.types.NotificationProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class AuthenticatorMessagingService extends FirebaseMessagingService {

    private static final String TAG = AuthenticatorMessagingService.class.getCanonicalName();

    /*
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        /*
         * save the FCM Registration Token to SharedPreferences for logging reasons
         */
        saveFcmRegistrationToken(token);

        /*
         * update the PingOne for Customer Android SDK with the new FCM token.
         */
        PingOne.setDeviceToken(this, token, NotificationProvider.FCM, pingOneSDKError -> {
            if (pingOneSDKError!=null){
                /*
                 * token update failed. You may re-schedule the update method or re-trigger it
                 * manually later with the token you've saved in the Shared Preferences.
                 */
                Log.e(TAG, "PingOne for Customer token update failed " + pingOneSDKError.getMessage());

            }
        });
    }

    @Override
    public void onMessageReceived(final @NonNull RemoteMessage remoteMessage) {
        Log.i(TAG, "Received FCM push message");
        PingOne.processRemoteNotification(this, remoteMessage, (notificationObject, pingOneSDKError) -> {

            if (notificationObject==null){
                //the push is not from PingOne For Customer - handle it your way
                Log.i(TAG, "Received push message is not from PingOne for Customer");
                /*
                 * implement the logic to handle push that was received from the other service than
                 * PingOne for Customer
                 */
                //...
                return;
            }

            /*
             * create intent to handle NotificationObject received from PingOne for Customer
             */
            Intent handleNotificationObjectIntent = createPingOneNotificationIntent(remoteMessage, notificationObject);
            /*
             * handle state where application was open when push was received
             */
            if(ProcessLifecycleOwner.get().getLifecycle().getCurrentState() == Lifecycle.State.RESUMED) {
                if (notificationObject.isTest()) {
                    /*
                     * received TEST push from PingOne service, you can choose to do nothing
                     */
                    Log.i(TAG, "PingOne for Customer Test push message received");
                } else {
                    /*
                     * received push message from PingOne for Customer while app is in foreground.
                     */
                    startActivity(handleNotificationObjectIntent);
                }
                /*
                 * handle state where application was closed/background when push was received
                 */
            }else{
                /*
                 * create notifications manager
                 */
                NotificationsManager notificationsManager = new NotificationsManager(AuthenticatorMessagingService.this);

                /*
                 * handle PingOne for Customers TEST push message
                 */
                if (notificationObject.isTest()){
                    notificationsManager.
                            buildAndSendPlainNotification(handleNotificationObjectIntent, false);
                    /*
                     * handle PingOne for Customers push message
                     */
                }else {
                    /*
                     * Parse category from remoteMessage data to build a notification accordingly.
                     * May be null if no category set at the server side.
                     */
                    handleNotificationObjectIntent.putExtra("category", remoteMessage.getData().get("category"));
                    notificationsManager.buildAndSendNotificationAccordingToCategory(handleNotificationObjectIntent);
                }
            }
        });
    }

    @NonNull
    private Intent createPingOneNotificationIntent(RemoteMessage remoteMessage, NotificationObject notificationObject){
        Intent handleNotificationObjectIntent = new Intent(AuthenticatorMessagingService.this, AuthenticationActivity.class);
        handleNotificationObjectIntent.setFlags(
                /*
                 * When using this flag, if a task is already running for the activity you are now
                 * starting, then a new activity will not be started; instead, the current task will
                 * simply be brought to the front of the screen with the state it was last in
                 */
                Intent.FLAG_ACTIVITY_NEW_TASK);
        /*
         * provide a NotificationObject received from PingOne for Customer SDK as a Parcelable
         * to the intent
         */
        handleNotificationObjectIntent.putExtra("PingOneNotificationObject", notificationObject);
        /*
         * Optional: parse title and message body from RemoteMessage and add it to the intent
         */
        parseTitleAndBody(remoteMessage, handleNotificationObjectIntent);

        return handleNotificationObjectIntent;
    }

    /*
     * Get title and body for the notification from "aps" key of RemoteMessage
     */
    private void parseTitleAndBody(@NonNull RemoteMessage remoteMessage, Intent intent){
        if(remoteMessage.getData().containsKey("aps")){
            try {
                JSONObject jsonObject = new JSONObject(Objects.requireNonNull(remoteMessage.getData().get("aps")));
                intent.putExtra("title", ((JSONObject)jsonObject.get("alert")).get("title").toString());
                intent.putExtra("body", ((JSONObject)jsonObject.get("alert")).get("body").toString());
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFcmRegistrationToken(@NonNull String token){
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("AuthenticatorSharedPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("FCM_TOKEN", token);
        editor.apply();
    }
}
