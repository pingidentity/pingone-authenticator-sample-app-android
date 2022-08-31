package com.pingidentity.authenticatorsampleapp.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.pingidentity.authenticatorsampleapp.BuildConfig;
import com.pingidentity.authenticatorsampleapp.QrAuthenticationActivity;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.adapters.SideMenuAdapter;
import com.pingidentity.authenticatorsampleapp.adapters.UsersAdapter;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.models.MainFragmentUserModel;
import com.pingidentity.authenticatorsampleapp.util.UserInterfaceUtil;
import com.pingidentity.authenticatorsampleapp.viewmodels.NetworkViewModel;
import com.pingidentity.authenticatorsampleapp.views.OneTimePasscodeView;
import com.pingidentity.pingidsdkv2.PingOne;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements UsersAdapter.AdapterSaveCallback, OneTimePasscodeView.PassCodeDataProvider {

    private DrawerLayout mDrawerLayout;
    private ProgressBar progressBar;
    private TextView notificationSliderTextView;
    private OneTimePasscodeView progressView;

    private LinkedHashMap<String, Pair<String, String>> localUsersArray;
    private final ArrayList<MainFragmentUserModel> usersArrayList = new ArrayList<>();
    private UsersAdapter adapter;
    private boolean shouldRetryPasscode = true;
    private boolean otpAnimationEnabled = true;


    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDrawerLayout = view.findViewById(R.id.side_menu_drawer);
        progressBar = view.findViewById(R.id.progress_bar_get_info);
        progressView = view.findViewById(R.id.passcode_view);

        Button button = view.findViewById(R.id.button_menu);
        button.setOnClickListener(view1 -> openDrawer());

        populateUsersView(view);
        populateButtons(view);
        populateMenuView(view);
        populateSupportIdView(view);
        populateVersionView(view);
        populateErrorSliderView(view);

        NetworkViewModel networkViewModel = new ViewModelProvider(requireActivity()).get(NetworkViewModel.class);
        networkViewModel.getNetwork().observe(requireActivity(), aBoolean -> UserInterfaceUtil.handleNetworkChange(aBoolean, requireContext(), notificationSliderTextView));

        final PreferencesManager preferencesManager = new PreferencesManager();
        localUsersArray = preferencesManager.getUsersList(requireContext());
        if (localUsersArray == null){
            localUsersArray = new LinkedHashMap<>();
        }
        progressBar.setVisibility(View.VISIBLE);
        final View innerView = view;
        PingOne.getInfo(requireContext(), (jsonObject, pingOneSDKError) -> {
            if (jsonObject!=null){
                usersArrayList.clear();
                JsonArray usersArray = jsonObject.getAsJsonArray("users");
                if (usersArray.size()==0){
                    //last user was unpaired from the server
                    preferencesManager.setIsDeviceActive(requireContext(), false);
                    requireActivity().runOnUiThread(() -> Navigation.findNavController(innerView)
                            .navigate(MainFragmentDirections.actionMainFragmentToCamera2Fragment()));
                }
                for(JsonElement user : usersArray){
                    MainFragmentUserModel mainFragmentUserModel = new Gson().fromJson(user, MainFragmentUserModel.class);
                    updateUserWithLocalBase(mainFragmentUserModel);
                    usersArrayList.add(mainFragmentUserModel);
                }
                invalidateLocalUsersWithRemote(preferencesManager);
                requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        populateOTP();
    }

    @Override
    public void onPause(){
        super.onPause();
        otpAnimationEnabled = false;
    }

    private void populateOTP() {
        otpAnimationEnabled = true;
        progressView.setPassCodeDataProvider(this);
        final ViewTreeObserver observer= progressView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                startOtpSequence();
                progressView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void populateButtons(View view) {

        Button addNewUserButton = view.findViewById(R.id.button_new_user);
        addNewUserButton.setOnClickListener(view1 -> {
            final NavController navController = Navigation.findNavController(view1);
            NavDirections navDirections = MainFragmentDirections.actionMainFragmentToCamera2Fragment2();
            navController.navigate(navDirections);
        });

        ImageButton qrAuth = view.findViewById(R.id.button_scan_qr_auth);
        qrAuth.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QrAuthenticationActivity.class)));


    }

    private void populateSupportIdView(View view){
        PreferencesManager preferencesManager = new PreferencesManager();
        String supportIdValue = preferencesManager.getSupportId(requireActivity());
        if (supportIdValue!=null){
            TextView textView = view.findViewById(R.id.side_menu_text_view_support_id);
            textView.setText(String.format(getString(R.string.main_view_support_id_placeholder), supportIdValue));
        }
    }

    private void populateVersionView(View view) {
        TextView textView = view.findViewById(R.id.side_menu_text_view_version);
        textView.setText(String.format(getString(R.string.main_view_version_placeholder), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    }


    private void populateMenuView(View view){

        final NavigationView navView = view.findViewById(R.id.side_menu_nav_view);
        navView.bringToFront();

        ListView menuList = view.findViewById(R.id.side_menu_list);
        menuList.setItemsCanFocus(true);

        ArrayList<MenuItem> arrayList = new ArrayList<>();
        PopupMenu p  = new PopupMenu(requireContext(), view);
        Menu menu = p.getMenu();
        requireActivity().getMenuInflater().inflate(R.menu.drawer_menu, menu);
        for (int i=0; i<menu.size(); i++){
            arrayList.add(menu.getItem(i));
        }
        SideMenuAdapter adapter = new SideMenuAdapter(requireActivity(), arrayList);
        menuList.setDivider(null);
        menuList.setAdapter(adapter);

        final View fragmentView = view;
        menuList.setOnItemClickListener((adapterView, view1, position, id) -> {
            closeDrawer();
            if (position==0){
                PingOne.sendLogs(requireContext(), (supportId, pingOneSDKError) -> {
                    if(supportId!=null){
                        requireActivity().runOnUiThread(() -> {
                            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.pop_up_logs_sent_title)
                                    .setMessage(getString(R.string.pop_up_logs_sent_message))
                                    .setPositiveButton(getString(R.string.pop_up_logs_sent_positive_button), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    })
                                    .create();
                        dialog.show();
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).
                                setTextColor(getResources().getColor(R.color.colorBlack, null));


                        PreferencesManager preferencesManager = new PreferencesManager();
                        preferencesManager.setSupportId(requireContext(), supportId);
                        populateSupportIdView(fragmentView);
                        });
                    }
                });
            }
        });
    }

    /*
     * load list of users and display them on the list view
     */
    private void populateUsersView(View view) {
        ListView userList = view.findViewById(R.id.list_view_users);
        adapter = new UsersAdapter(requireContext(), usersArrayList, this);
        userList.setAdapter(adapter);
        userList.setDivider(null);
    }

    /*
     * Prepare the error slider view to show errors in real time
     */
    private void populateErrorSliderView(View view){
        notificationSliderTextView = view.findViewById(R.id.text_notification_slider);
    }

    private void updateUserWithLocalBase(MainFragmentUserModel mainFragmentUserModel){
        if (localUsersArray.isEmpty() || !localUsersArray.containsKey(mainFragmentUserModel.getId())){
            addRemoteUserToLocalBase(mainFragmentUserModel);
        }else{
            Pair<String, String> p = localUsersArray.get(mainFragmentUserModel.getId());
            if (p==null){
                p = new Pair<>("","");
            }
            if(mainFragmentUserModel.getUsername()==null){
                mainFragmentUserModel.setUsername(new MainFragmentUserModel().new Username("", ""));
                localUsersArray.put(mainFragmentUserModel.getId(), new Pair<>(p.first, ""));
            }else{
                if(!mainFragmentUserModel.getUsername().getGiven().equals(p.second)){
                    localUsersArray.put(mainFragmentUserModel.getId(), new Pair<>(p.first, mainFragmentUserModel.getUsername().getGiven()));
                }
            }
            mainFragmentUserModel.getUsername().setGiven(Objects.requireNonNull(localUsersArray.get(mainFragmentUserModel.getId())).first);
            mainFragmentUserModel.setNickname(Objects.requireNonNull(localUsersArray.get(mainFragmentUserModel.getId())).second);
        }
    }

    private void updateLocalBaseWithEditedUser(MainFragmentUserModel mainFragmentUserModel){
        if (!mainFragmentUserModel.getUsername().getGiven().isEmpty()) {
            localUsersArray.put(mainFragmentUserModel.getId(), new Pair<>(mainFragmentUserModel.getUsername().getGiven(), mainFragmentUserModel.getNickname()));
        }else{
            localUsersArray.put(mainFragmentUserModel.getId(), new Pair<>(mainFragmentUserModel.getNickname(), mainFragmentUserModel.getNickname()));
        }

    }

    //add the mainFragmentUserModel object received from the server to local array
    private void addRemoteUserToLocalBase(MainFragmentUserModel mainFragmentUserModel){
        adapter.notifyDataSetChanged(mainFragmentUserModel.getId());
        if (mainFragmentUserModel.getUsername()==null || mainFragmentUserModel.getUsername().getGiven()==null){
            mainFragmentUserModel.setUsername(new MainFragmentUserModel().new Username("", ""));
        }
        mainFragmentUserModel.setNickname(mainFragmentUserModel.getUsername().getGiven());
        localUsersArray.put(mainFragmentUserModel.getId(), new Pair<>(mainFragmentUserModel.getUsername().getGiven(), mainFragmentUserModel.getUsername().getGiven()));
    }

    private void invalidateLocalUsersWithRemote(PreferencesManager preferencesManager){
        Iterator<Map.Entry<String, Pair<String, String>>> it = localUsersArray.entrySet().iterator();
        outer:
        while(it.hasNext()){
            Map.Entry<String, Pair<String, String>> localUser = it.next();
            for (MainFragmentUserModel mainFragmentUserModel : usersArrayList){
                if (localUser.getKey().equals(mainFragmentUserModel.getId())){
                    continue outer;
                }
            }
            it.remove();
        }
        preferencesManager.storeUsersList(requireContext(), localUsersArray);
    }

    /*
     * open the drawer in a smooth way after hamburger pressed
     */
    private void openDrawer(){
        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mDrawerLayout.isDrawerOpen(GravityCompat.END))
                    mDrawerLayout.openDrawer(GravityCompat.END);
            }
        };
        new Handler().postDelayed(mPendingRunnable, 300);
    }

    /*
     * close the drawer in a smooth way after selecting option
     */
    private void closeDrawer() {
        Runnable mPendingRunnable = () -> {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.END))
                mDrawerLayout.closeDrawer(GravityCompat.END);
        };
        new Handler().postDelayed(mPendingRunnable, 300);
    }

    @Override
    public void onSave(MainFragmentUserModel editedMainFragmentUserModel) {
        updateLocalBaseWithEditedUser(editedMainFragmentUserModel);
        PreferencesManager preferencesManager = new PreferencesManager();
        preferencesManager.storeUsersList(requireContext(), localUsersArray);
    }

    private void startOtpSequence(){
        if(!progressView.isWorking()){
            PingOne.getOneTimePassCode(progressView.getContext(), (otpData, error) -> {
                if(otpData!=null){
                    progressView.updatePassCode(otpData);
                }else{
                    progressView.setVisibility(View.INVISIBLE);
                    if(shouldRetryPasscode){
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(this::startOtpSequence, 5000);
                        shouldRetryPasscode=false;
                    }
                }

            });
        }

    }

    @Override
    public void onPassCodeExpired() {
        if(otpAnimationEnabled){
            startOtpSequence();
        }
    }

    @Override
    public void onCopyToClipboard() {
        UserInterfaceUtil.promptMessage(progressView.getContext(), notificationSliderTextView, getString(R.string.copied));
    }

}
