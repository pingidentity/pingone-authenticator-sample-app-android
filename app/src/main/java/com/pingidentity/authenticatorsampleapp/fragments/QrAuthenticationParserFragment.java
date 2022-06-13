package com.pingidentity.authenticatorsampleapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.adapters.QrAuthUserListAdapter;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.models.QrAuthUserModel;
import com.pingidentity.authenticatorsampleapp.util.UserInterfaceUtil;
import com.pingidentity.pingidsdkv2.AuthenticationObject;
import com.pingidentity.pingidsdkv2.PingOne;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public class QrAuthenticationParserFragment extends Fragment implements QrAuthUserListAdapter.UserSelectCallback {

    private AuthenticationObject authenticationObject;
    private RelativeLayout verifyingLayout;
    private RelativeLayout successLayout;
    private RelativeLayout timeoutLayout;
    private RelativeLayout errorLayout;
    private ImageView spinnerImage;
    private TextView errorTextView;
    private TextView titleTextView;
    private TextView subtitleTextView;

    private Button approveButton;
    private Button denyButton;
    ListView userListView;

    private final ArrayList<QrAuthUserModel> usersArrayList = new ArrayList<>();
    private LinkedHashMap<String, Pair<String, String>> localUsersArray;

    private QrAuthUserListAdapter adapter;

    public QrAuthenticationParserFragment(){
        //required empty c-tor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qr_authentication_parsing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        verifyingLayout = view.findViewById(R.id.layout_verifying);
        TextView verifyingText = view.findViewById(R.id.text_view_verifying);
        verifyingText.setText(R.string.qr_authentication_spinner_text);
        spinnerImage = view.findViewById(R.id.image_spinner);
        successLayout = view.findViewById(R.id.layout_success_qr_scan);
        timeoutLayout = view.findViewById(R.id.layout_qr_expired);
        errorLayout = view.findViewById(R.id.layout_invalid);
        errorTextView = view.findViewById(R.id.text_view_invalid);
        titleTextView = view.findViewById(R.id.text_view_qr_auth_title);
        subtitleTextView = view.findViewById(R.id.text_view_qr_auth_subtitle);
        initializeButtons(view);

        userListView = view.findViewById(R.id.list_view_users_qr_auth);
        userListView.setDivider(null);
        if (getArguments()!=null && getArguments().containsKey("qrCodeContent")) {
            showVerifyingLayout();
            authenticate(getArguments().getString("qrCodeContent"));
        }else{
            hideVerifyingLayout();
            showErrorLayout(getResources().getString(R.string.generic_string_error));
        }
    }

    private void initializeButtons(View view){
        //initialize buttons
        approveButton = view.findViewById(R.id.qr_auth_button_approve);
        approveButton.setOnClickListener(v -> approveAuthentication());
        denyButton = view.findViewById(R.id.qr_auth_button_deny);
        denyButton.setOnClickListener(v -> denyAuthentication());
    }

    private void authenticate(String qrCodeInput) {
        PingOne.authenticate(requireContext(), qrCodeInput,
                (authenticationObject, pingOneSDKError) -> {
            hideVerifyingLayout();
            if(pingOneSDKError!=null){
                showErrorLayout(UserInterfaceUtil.
                        handlePingOneSDKErrorMessage(requireContext(), pingOneSDKError));
            }else{
                if(authenticationObject!=null) {
                    this.authenticationObject = authenticationObject;
                    parseAuthenticationObject();
                }else{
                    /*
                     * should never get there, authenticationObject and PingOneSDKError
                     * cannot be null at the same time
                     */
                    showErrorLayout(getResources().getString(R.string.generic_string_error));
                }
            }
        });
    }

    private void parseAuthenticationObject(){
        switch (authenticationObject.getStatus().toUpperCase()){
            case "COMPLETED":
                showSuccessLayout();
                break;
            case "EXPIRED":
                showTimeoutLayout();
                break;
                //following case should never be returned in this flow
            case "DENIED":
                showErrorLayout(getResources().getString(R.string.qr_view_denied_layout_message));
                break;
            case "CLAIMED":
                requireActivity().runOnUiThread(() ->
                        handleClaimedStatusOfAuthenticationObject(authenticationObject));
                break;
            default:
                showErrorLayout(getResources().getString(R.string.generic_string_error));
                break;

        }
    }

    private void handleClaimedStatusOfAuthenticationObject(AuthenticationObject authenticationObject) {
        if (authenticationObject.getUsers().size()==1 &&
                authenticationObject.getNeedsApproval().equalsIgnoreCase("NOT_REQUIRED")){
            /*
             * Should never get here. In case of single user when user approval is not required
             * the server should never send "CLAIMED" status, so this should never happen.
             */
            showErrorLayout(getResources().getString(R.string.generic_string_error));
        }else{
            fillUsersList();
        }
    }

    private void fillUsersList(){
        //show corresponding title and subtitle according to the number of users
        if (authenticationObject.getUsers().size()==1){
            titleTextView.setText(R.string.qr_auth_title_single_user);
            approveButton.setEnabled(true);
            approveButton.setTextColor(getResources().getColor(R.color.color_approve_button_enabled, null));
        }else{
            titleTextView.setText(R.string.qr_auth_title_multiply_users);
            subtitleTextView.setVisibility(View.VISIBLE);
        }
        //invalidate user appearance according to local stored user list
        PreferencesManager preferencesManager = new PreferencesManager();
        localUsersArray = preferencesManager.getUsersList(requireActivity());
        if (localUsersArray == null){
            localUsersArray = new LinkedHashMap<>();
        }
        for (JsonElement user : authenticationObject.getUsers()){
            QrAuthUserModel userModel = new Gson().fromJson(user, QrAuthUserModel.class);
            invalidateUserWithLocalBase(userModel);
            usersArrayList.add(userModel);
        }

        adapter = new QrAuthUserListAdapter(requireActivity(), usersArrayList, this);
        userListView.setAdapter(adapter);
    }

    /*
     * if end-user has edited the username, given name or family name in the main
     * application fragment, than we want to represent it in the list exactly as in
     * main fragment's list view for better user experience
     */
    private void invalidateUserWithLocalBase(QrAuthUserModel user) {
        if (localUsersArray.isEmpty() || !localUsersArray.containsKey(user.getId())){
            /*
             * user doesn't exists in stored users list. Add it as is to the current array list
             * which will be represented to the end-user
             */
            usersArrayList.add(user);
        }else if (localUsersArray.containsKey(user.getId())){
            /*
             * the user exists in the local users array. Make sure it is represented exactly
             * as in the main fragment of the application
             */
            Pair<String, String> p = localUsersArray.get(user.getId());
            if (p==null){
                p = new Pair<>("","");
            }
            if(user.getFullName()==null) {
                user.setFullName(new QrAuthUserModel().new FullName("", ""));
            }
            user.getFullName().setGiven(Objects.requireNonNull(localUsersArray.get(user.getId())).first);
            user.setUsername(Objects.requireNonNull(localUsersArray.get(user.getId())).second);
        }
    }

    private void approveAuthentication(){
        authenticate(true);
    }


    private void denyAuthentication() {
        authenticate(false);
    }

    private void authenticate(boolean isApproved){
        showVerifyingLayout();
        PingOne.PingOneAuthenticationStatusCallback callback =
                buildPingOneAuthenticationStatusCallback();
        if (isApproved){
            String userId;
            if (authenticationObject.getUsers().size()>1) {
                userId = authenticationObject.getUsers().
                        get(adapter.getSelectedPosition()).getAsJsonObject().get("id").getAsString();
            }else{
                userId = authenticationObject.getUsers().
                        get(0).getAsJsonObject().get("id").getAsString();
            }
            authenticationObject.approve(requireContext(), userId, callback);
        }else{
            authenticationObject.deny(requireContext(), null, callback);
        }
    }

    //build a callback to the approve/deny request
    private PingOne.PingOneAuthenticationStatusCallback buildPingOneAuthenticationStatusCallback(){
        return (status, pingOneSDKError) -> {
            hideVerifyingLayout();
            if (pingOneSDKError!=null){
                showErrorLayout(UserInterfaceUtil
                        .handlePingOneSDKErrorMessage(requireContext(), pingOneSDKError));
            }else if(status!=null){
                switch (status){
                    case "COMPLETED":
                        showSuccessLayout();
                        break;
                    case "DENIED":
                        showErrorLayout(getResources().getString(R.string.qr_view_denied_layout_message));
                        break;
                    case "EXPIRED":
                        showTimeoutLayout();
                        break;
                    default:
                        showErrorLayout(getResources().getString(R.string.generic_string_error));
                        break;

                }
            }
        };
    }

    private void showVerifyingLayout(){
        drawStatusBar(getResources().getColor(R.color.layout_verifying_background_color, null));
        verifyingLayout.setVisibility(View.VISIBLE);
        approveButton.setEnabled(false);
        denyButton.setEnabled(false);
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setDuration(2000);
        rotate.setRepeatCount(Animation.INFINITE);
        spinnerImage.startAnimation(rotate);
    }

    private void hideVerifyingLayout(){
        requireActivity().runOnUiThread(() -> {
            drawStatusBar(getResources().
                    getColor(R.color.color_toolbar_background, null));
            verifyingLayout.setVisibility(View.GONE);
            denyButton.setEnabled(true);
        });
    }

    private void showErrorLayout(String errorMessage){
        requireActivity().runOnUiThread(() -> {
            drawStatusBar(getResources().
                    getColor(R.color.layout_invalid_background_color, null));
            errorLayout.setVisibility(View.VISIBLE);
            approveButton.setEnabled(false);
            denyButton.setEnabled(false);
            errorTextView.setText(errorMessage);
        });
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                requireActivity().finish(), 3500);
    }

    private void showTimeoutLayout(){
        requireActivity().runOnUiThread(() -> {
            drawStatusBar(getResources().
                    getColor(R.color.layout_timed_out_background_color, null));
            timeoutLayout.setVisibility(View.VISIBLE);
            approveButton.setEnabled(false);
            denyButton.setEnabled(false);
        });
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                requireActivity().finish(), 3500);
    }

    private void showSuccessLayout(){
        requireActivity().runOnUiThread(() -> {
            drawStatusBar(getResources().
                    getColor(R.color.layout_success_background_color, null));
            approveButton.setEnabled(false);
            denyButton.setEnabled(false);
            successLayout.setVisibility(View.VISIBLE);
        });
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                requireActivity().finish(), 3500);
    }

    //set color of the status bar according to the underlying view
    private void drawStatusBar(int color){
        Window window = requireActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(color);
    }

    @Override
    public void onUserSelected() {
        approveButton.setEnabled(true);
        approveButton.setTextColor(getResources().getColor(R.color.color_approve_button_enabled, null));
    }
}
