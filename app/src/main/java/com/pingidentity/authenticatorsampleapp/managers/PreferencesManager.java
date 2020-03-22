package com.pingidentity.authenticatorsampleapp.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class PreferencesManager {

    //the filename to store prefs
    private static final String PREFERENCES_FILENAME = "authenticator_prefs";
    //keys to the stored prefs
    private static final String SUPPORT_ID_KEY = "support_id";
    private static final String IS_DEVICE_ACTIVE = "is_device_active";
    private static final String USERS_LIST = "users_list";

    public PreferencesManager(){}

    private void addEntry(Context context, String key, String value){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key, value).apply();
    }

    private String getEntry(Context context, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    private void addBooleanEntry(Context context, String key, boolean value){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private boolean getBooleanEntry(Context context, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, false);
    }

    public void setSupportId(Context context, String value){
        addEntry(context, SUPPORT_ID_KEY, value);
    }

    public String getSupportId(Context context){
        return getEntry(context, SUPPORT_ID_KEY);
    }

    public void setIsDeviceActive(Context context, boolean deviceActive){
        addBooleanEntry(context, IS_DEVICE_ACTIVE, deviceActive);
    }

    public boolean isDeviceActive(Context context){
        return getBooleanEntry(context, IS_DEVICE_ACTIVE);
    }

    public void storeUsersList(Context context, LinkedHashMap<String, Pair<String, String>> value){
        String usersListAsJson = new Gson().toJson(value);
        System.out.println(usersListAsJson);
        addEntry(context, USERS_LIST, new Gson().toJson(value));
    }

    public LinkedHashMap<String, Pair<String, String>> getUsersList(Context context){
        String usersListAsString = getEntry(context, USERS_LIST);
        LinkedHashMap<String, Pair<String, String>> users;
        Type type = new TypeToken<LinkedHashMap<String, Pair<String, String>>>() {}.getType();
        users = new Gson().fromJson(usersListAsString, type);
        System.out.println("USERS : " + users);

        return users;
    }
}
