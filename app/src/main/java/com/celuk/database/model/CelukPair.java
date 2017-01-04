package com.celuk.database.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by adikwidiasmono on 11/13/16.
 */

public class CelukPair {
    public static final String PAIR_STATUS_ACTIVE = "ACTIVE";
    public static final String PAIR_STATUS_INACTIVE = "INACTIVE";

    @SerializedName("caller")
    @Expose
    private String caller;
    @SerializedName("paired_date")
    @Expose
    private String pairedDate;
    @SerializedName("receiver")
    @Expose
    private String receiver;
    @SerializedName("status")
    @Expose
    private String status;

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
     * @return The pairedDate
     */
    public String getPairedDate() {
        return pairedDate;
    }

    /**
     * @param pairedDate The paired_date
     */
    public void setPairedDate(String pairedDate) {
        this.pairedDate = pairedDate;
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
