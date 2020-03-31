package com.pingidentity.authenticatorsampleapp.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pingidentity.authenticatorsampleapp.BuildConfig;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.adapters.SideMenuAdapter;
import com.pingidentity.authenticatorsampleapp.adapters.UsersAdapter;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.models.User;
import com.pingidentity.authenticatorsampleapp.util.UserInterfaceUtil;
import com.pingidentity.authenticatorsampleapp.viewmodels.NetworkViewModel;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements UsersAdapter.AdapterSaveCallback {

    private DrawerLayout mDrawerLayout;
    private ProgressBar progressBar;
    private TextView notificationSliderTextView;

    private LinkedHashMap<String, Pair<String, String>> localUsersArray;
    private ArrayList<User> usersArrayList = new ArrayList<>();
    private UsersAdapter adapter;

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

        Button button = view.findViewById(R.id.button_menu);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDrawer();
            }
        });
        populateUsersView(view);
        populateNewButton(view);
        populateMenuView(view);
        populateSupportIdView(view);
        populateVersionView(view);
        populateErrorSliderView(view);

        NetworkViewModel networkViewModel = new ViewModelProvider(requireActivity()).get(NetworkViewModel.class);
        networkViewModel.getNetwork().observe(requireActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                UserInterfaceUtil.handleNetworkChange(aBoolean, requireContext(), notificationSliderTextView);
            }
        });

        final PreferencesManager preferencesManager = new PreferencesManager();
        localUsersArray = preferencesManager.getUsersList(requireContext());
        if (localUsersArray == null){
            localUsersArray = new LinkedHashMap<>();
        }
        progressBar.setVisibility(View.VISIBLE);
        final View innerView = view;
        PingOne.getInfo(requireContext(), new PingOne.PingOneGetInfoCallback() {
            @Override
            public void onComplete(@Nullable JsonObject jsonObject, @Nullable PingOneSDKError pingOneSDKError) {
                if (jsonObject!=null){
                    usersArrayList.clear();
                    JsonArray usersArray = jsonObject.getAsJsonArray("users");
                    if (usersArray.size()==0){
                        //last user was unpaired from the server
                        preferencesManager.setIsDeviceActive(requireContext(), false);
                        Navigation.findNavController(innerView)
                                .navigate(MainFragmentDirections.actionMainFragmentToCamera2Fragment2());

                    }
                    for(JsonElement user : usersArray){
                        User user1 = new Gson().fromJson(user, User.class);
                        updateUserWithLocalBase(user1);
                        usersArrayList.add(user1);
                    }
                    invalidateLocalUsersWithRemote(preferencesManager);
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }



    private void populateNewButton(View view) {

        Button addNewUserButton = view.findViewById(R.id.button_new_user);
        addNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final NavController navController = Navigation.findNavController(view);
                NavDirections navDirections = MainFragmentDirections.actionMainFragmentToCamera2Fragment2();
                navController.navigate(navDirections);
            }
        });

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
        SideMenuAdapter adapter = new SideMenuAdapter(requireContext(), arrayList);
        menuList.setDivider(null);
        menuList.setAdapter(adapter);

        final View fragmentView = view;
        menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int position, long id) {
                closeDrawer();
                if (position==0){
                    PingOne.sendLogs(requireContext(), new PingOne.PingOneSendLogsCallback() {
                        @Override
                        public void onComplete(@Nullable final String supportId, @Nullable PingOneSDKError pingOneSDKError) {
                            if(supportId!=null){
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(requireContext())
                                                .setMessage(getString(R.string.pop_up_logs_sent_message))
                                                .setPositiveButton(getString(R.string.pop_up_logs_sent_positive_button), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        dialogInterface.dismiss();
                                                    }
                                                })
                                                .create()
                                                .show();
                                        PreferencesManager preferencesManager = new PreferencesManager();
                                        preferencesManager.setSupportId(requireContext(), supportId);
                                        populateSupportIdView(fragmentView);
                                    }
                                });
                            }
                        }
                    });
                }
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

    private void updateUserWithLocalBase(User user){
        if (localUsersArray.isEmpty() || !localUsersArray.containsKey(user.getId())){
            addRemoteUserToLocalBase(user);
        }else{
            Pair<String, String> p = localUsersArray.get(user.getId());
            if(user.getUsername()==null){
                user.setUsername(new User().new Username("", ""));
                localUsersArray.put(user.getId(), new Pair<String, String>(p.first, ""));
            }else{
                if(!user.getUsername().getGiven().equals(p.second)){
                    localUsersArray.put(user.getId(), new Pair<>(p.first, user.getUsername().getGiven()));
                }
            }
            user.getUsername().setGiven(Objects.requireNonNull(localUsersArray.get(user.getId())).first);
            user.setNickname(Objects.requireNonNull(localUsersArray.get(user.getId())).second);
        }
    }

    private void updateLocalBaseWithEditedUser(User user){
        if (!user.getUsername().getGiven().isEmpty()) {
            localUsersArray.put(user.getId(), new Pair<>(user.getUsername().getGiven(), user.getNickname()));
        }else{
            localUsersArray.put(user.getId(), new Pair<>(user.getNickname(), user.getNickname()));
        }

    }

    /**
     * adds the user object received from the server to local array
     * @param user
     */
    private void addRemoteUserToLocalBase(User user){
        adapter.notifyDataSetChanged(user.getId());
        if (user.getUsername()==null || user.getUsername().getGiven()==null){
            user.setUsername(new User().new Username("", ""));
        }
        user.setNickname(user.getUsername().getGiven());
        localUsersArray.put(user.getId(), new Pair<>(user.getUsername().getGiven(), user.getUsername().getGiven()));
    }

    private void invalidateLocalUsersWithRemote(PreferencesManager preferencesManager){
        Iterator<Map.Entry<String, Pair<String, String>>> it = localUsersArray.entrySet().iterator();
        outer:
        while(it.hasNext()){
            Map.Entry<String, Pair<String, String>> localUser = it.next();
            for (User user : usersArrayList){
                if (localUser.getKey().equals(user.getId())){
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
        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.END))
                    mDrawerLayout.closeDrawer(GravityCompat.END);
            }
        };
        new Handler().postDelayed(mPendingRunnable, 300);
    }

    @Override
    public void onSave(User editedUser) {
        updateLocalBaseWithEditedUser(editedUser);
        PreferencesManager preferencesManager = new PreferencesManager();
        preferencesManager.storeUsersList(requireContext(), localUsersArray);
    }
}
