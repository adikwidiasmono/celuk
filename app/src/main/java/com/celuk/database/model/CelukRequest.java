package com.celuk.database.model;

import com.google.firebase.database.Exclude;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by adikwidiasmono on 11/13/16.
 */

public class CelukRequest {
    public static final String REQUEST_STATUS_PENDING = "PENDING";
    public static final String REQUEST_STATUS_ACCEPT = "ACCEPT";
    public static final String REQUEST_STATUS_REJECT = "REJECT";
    public static final String REQUEST_STATUS_HISTORY = "HISTORY";

    @SerializedName("caller")
    @Expose
    private String caller;
    @SerializedName("requested_date")
    @Expose
    private String requestedDate;
    @SerializedName("response_date")
    @Expose
    private String responseDate;
    @SerializedName("receiver")
    @Expose
    private String receiver;
    @SerializedName("status")
    @Expose
    private String status;

    public CelukRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(CelukRequest.class)
    }

    public CelukRequest(String caller, String requestedDate, String receiver, String status) {
        this.caller = caller;
        this.requestedDate = requestedDate;
        this.receiver = receiver;
        this.status = status;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("caller", caller);
        result.put("receiver", receiver);
        result.put("requested_date", requestedDate);
        result.put("status", status);

        return result;
    }
    // [END post_to_map]


    @Override
    public String toString() {
        return caller + ":" + receiver + ":"
                + ":" + requestedDate + ":" + status;
    }

    /**
     * @return The caller
     */
    public String getCaller() {
        return caller;
    }

    /**
     * @param caller The caller
     */
    public void setCaller(String caller) {
        this.caller = caller;
    }

    /**
     * @return The requestedDate
     */
    public String getRequestedDate() {
        return requestedDate;
    }

    /**
     * @param requestedDate The requested_date
     */
    public void setRequestedDate(String requestedDate) {
        this.requestedDate = requestedDate;
    }

    /**
     * @return The responseDate
     */
    public String getResponseDate() {
        return responseDate;
    }

    /**
     * @param responseDate The response_date
     */
    public void setResponseDate(String responseDate) {
        this.responseDate = responseDate;
    }

    /**
     * @return The receiver
     */
    public String getReceiver() {
        return receiver;
    }

    /**
     * @param receiver The receiver
     */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    /**
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status The status
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
