package com.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.celuk.database.model.CelukUser;
import com.google.gson.Gson;

/**
 * Created by adikwidiasmono on 10/23/16.
 */

public class CelukSharedPref {
    private final String SHARED_PREFERENCE_NAME = "com.adik.celuk";
    private final String CURRENT_USER = "currentUser";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
//    private final String CURRENT_CALLER = "currentCaller";
//    private final String CURRENT_RECEIVER = "currentCaller";

    public CelukSharedPref(Context ctx) {
        prefs = ctx.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void clearAll() {
        editor.clear().commit();
    }

    public CelukUser getCurrentUser() {
        String userString = prefs.getString(CURRENT_USER, null);
        CelukUser user;
        if (userString == null) {
            user = new CelukUser();
        } else {
            user = new Gson().fromJson(userString, CelukUser.class);
        }
        return user;
    }

    public void setCurrentUser(CelukUser currentUser) {
        String userString = new Gson().toJson(currentUser);
        prefs.edit().putString(CURRENT_USER, userString).apply();
    }

//    public CelukUser getCurrentCaller() {
//        String callerString = prefs.getString(CURRENT_CALLER, null);
//        CelukUser caller;
//        if (callerString == null) {
//            caller = new CelukUser();
//        } else {
//            caller = new Gson().fromJson(callerString, CelukUser.class);
//        }
//        return caller;
//    }
//
//    public void setCurrentCaller(CelukUser currentCaller) {
//        String callerString = new Gson().toJson(currentCaller);
//        prefs.edit().putString(CURRENT_CALLER, callerString).apply();
//    }
//
//    public CelukUser getCurrentReceiver() {
//        String receiverString = prefs.getString(CURRENT_RECEIVER, null);
//        CelukUser receiver;
//        if (receiverString == null) {
//            receiver = new CelukUser();
//        } else {
//            receiver = new Gson().fromJson(receiverString, CelukUser.class);
//        }
//        return receiver;
//    }
//
//    public void setCurrentReceiver(CelukUser currentReceiver) {
//        String receiverString = new Gson().toJson(currentReceiver);
//        prefs.edit().putString(CURRENT_RECEIVER, receiverString).apply();
//    }

}
