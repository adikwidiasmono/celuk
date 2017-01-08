package com.utils;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.celuk.caller.CallerActivity;
import com.celuk.database.model.CelukUser;
import com.celuk.main.MainActivity;
import com.celuk.receiver.ReceiverActivity;

/**
 * Created by adikwidiasmono on 11/19/16.
 */

public class AppUtils {

    public static boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    public static void routeCelukUser(Activity activity, CelukUser celukUser) {
        if (celukUser == null)
            return;

        Class actionClass = null;
        if (TextUtils.isEmpty(celukUser.getRequestId())) {
            actionClass = MainActivity.class;
        } else {
            switch (celukUser.getPairedState()) {
                case CelukState.CELUK_NO_ASSIGNMENT:
                    actionClass = MainActivity.class;
                    break;
                case CelukState.CALLER_READY:
                    actionClass = CallerActivity.class;
                    break;
                case CelukState.CALLER_CALL_RECEIVER:
                    actionClass = CallerActivity.class;
                    break;
                case CelukState.CALLER_WAIT_RECEIVER:
                    actionClass = CallerActivity.class;
                    break;
                case CelukState.RECEIVER_READY:
                    actionClass = ReceiverActivity.class;
                    break;
//                case CelukState.RECEIVER_GET_CALL:
//                    actionClass = ReceiverActivity.class;
//                    break;
                case CelukState.RECEIVER_ACCEPT_CALL:
                    actionClass = ReceiverActivity.class;
                    break;
                default:
                    actionClass = MainActivity.class;
                    break;
            }
        }


        Intent intent = new Intent(activity.getApplicationContext(), actionClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
    }
}
