package com.utils;

/**
 * Created by adikwidiasmono on 10/26/16.
 */

public class CelukState {
    // User have no assignment as CALLER nor RECEIVER
    public final static int CELUK_NO_ASSIGNMENT = 0;

    // User active as CALLER
    public final static int CALLER_READY = 1; // CALLER ready to call RECEIVER
    public final static int CALLER_CALL_RECEIVER = 2; // CALLER call RECEIVER but RECEIVER hasn't answered its call
    public final static int CALLER_WAIT_RECEIVER = 3; // RECEIVER has accepted its call, CALLER track RECEIVER

    // User active as RECEIVER
    public final static int RECEIVER_READY = 4; // RECEIVER ready to get a call by CALLER
    public final static int RECEIVER_GET_CALL = 5; // RECEIVER get a call but hasn't answered it
    public final static int RECEIVER_ACCEPT_CALL = 6; // RECEIVER accept CALLER's call then go to CALLER place
}