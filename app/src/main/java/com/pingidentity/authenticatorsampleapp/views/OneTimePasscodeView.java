package com.pingidentity.authenticatorsampleapp.views;


import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.pingidsdkv2.types.OneTimePasscodeInfo;


public class OneTimePasscodeView extends ConstraintLayout implements View.OnClickListener {

    public interface PassCodeDataProvider{
        void onPassCodeExpired();
        void onCopyToClipboard();
    }

    private final int OTP_ABOUT_TO_EXPIRE_POINT = 66;
    private final int OTP_GET_NEW_CODE = 99;
    private final int MILLIS = 1000;

    OneTimePasscodeInfo otpData;
    TextView passCode;
    View animatedWrapper;
    View animated;
    PassCodeDataProvider listener;



    public OneTimePasscodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View container = inflater.inflate(R.layout.one_time_passcode_view, this, true);
        passCode = container.findViewById(R.id.tv_passcode);
        animatedWrapper = container.findViewById(R.id.otp_progress);
        animated = container.findViewById(R.id.progress);
    }

    @Override
    public void onClick(View v) {
        if(otpData!=null && otpData.getPasscode()!=null) {
            setToClipboard(v.getContext(), otpData.getPasscode());
            if(listener!=null){
                listener.onCopyToClipboard();
            }
        }
    }

    public void setPassCodeDataProvider(PassCodeDataProvider listener){
        this.listener = listener;
    }

    public void updatePassCode(OneTimePasscodeInfo otpData){
        this.otpData = otpData;
        handleAnimation(this, otpData);
    }

    private void setToClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(text, text);
        clipboard.setPrimaryClip(clip);
    }

    private void handleAnimation(final View progress, OneTimePasscodeInfo otpData){
        passCode.setText(otpData.getPasscode());
        animated.setBackgroundColor(getResources().getColor(R.color.otp_view_progress_normal));
        passCode.setTextColor(getResources().getColor(R.color.otp_view_passcode));


        long runTimeMillis = (long) (otpData.getValidUntil()*MILLIS - System.currentTimeMillis());
        int runTimeSeconds = (int)(runTimeMillis/MILLIS);
        double remainingRuntimePercent = (double) runTimeSeconds / (double) otpData.getTimeWindowSize() * 100;
        double relativeHeight = remainingRuntimePercent / 100;


        ViewGroup.LayoutParams lp = animated.getLayoutParams();
        lp.height = (int) (animatedWrapper.getHeight() * relativeHeight);
        animated.setLayoutParams(lp);

        //init the colors to the correct state
        int currentProgressPercent = (int) (100 - remainingRuntimePercent);
        if(currentProgressPercent > OTP_ABOUT_TO_EXPIRE_POINT){
            animated.setBackgroundColor(getResources().getColor(R.color.redLight));
            passCode.setTextColor(getResources().getColor(R.color.redLight));
        }

        ValueAnimator anim = ValueAnimator.ofInt(lp.height, 0);
        anim.addUpdateListener(valueAnimator -> {
            int val = (int) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = animated.getLayoutParams();
            layoutParams.height = val;

            animated.setLayoutParams(layoutParams);
            long percent = 100*valueAnimator.getCurrentPlayTime()/valueAnimator.getDuration();

            if(percent > OTP_ABOUT_TO_EXPIRE_POINT) {
                animated.setBackgroundColor(getResources().getColor(R.color.redLight));
                passCode.setTextColor(getResources().getColor(R.color.redLight));
            }
            if(percent > OTP_GET_NEW_CODE){
                if(listener!=null){
                    listener.onPassCodeExpired();
                }

            }
        });

        anim.setDuration(runTimeMillis);
        anim.start();
        progress.setVisibility(View.VISIBLE);

    }
}
