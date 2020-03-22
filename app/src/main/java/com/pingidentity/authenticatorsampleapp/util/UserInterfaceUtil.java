package com.pingidentity.authenticatorsampleapp.util;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


import com.pingidentity.authenticatorsampleapp.R;

/*
 * This class will contain methods that any activity/fragment
 * can use.
 */
public class UserInterfaceUtil {

    public static void handleNetworkChange(Boolean networkOnline, Context context, TextView textView){
        if(networkOnline){
            hideNoNetworkSlider(textView);
        }else{
            displayNoNetworkSlider(context, textView);
        }
    }

    private static void hideNoNetworkSlider(TextView textView){
        if(textView.getVisibility()==View.GONE){
            return;
        }
        textView.setVisibility(View.GONE);
    }

    private static void displayNoNetworkSlider(Context context, TextView textView){
        if(textView.getVisibility()==View.VISIBLE){
            return;
        }
        textView.setText(R.string.main_screen_notification_no_network);
        textView.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_down);
        textView.startAnimation(animation);
    }
}
