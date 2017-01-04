package com.celuk.database.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by adikwidiasmono on 10/26/16.
 */

public class CelukUser {
//    public static final Integer PAIR_STAT_CALLER_READY = 0; // CALLER ready
//    public static final Integer PAIR_STAT_CALLER_SEND_A_CALL = 1; // CALLER call a receiver but RECEIVER hasn't response it call
//    public static final Integer PAIR_STAT_CALLER_WAIT = 2; // CALLER wait RECEIVER after RECEIVER response it call
//
//    public static final Integer PAIR_STAT_RECEIVER_READY = 3; // RECEIVER ready
//    public static final Integer PAIR_STAT_RECEIVER_GET_A_CALL = 4; // RECEIVER receive a call from a CALLER but hasn't response it
//    public static final Integer PAIR_STAT_RECEIVER_ACCEPT_A_CALL = 5; // RECEIVER response a call and go to RECEIVER place

    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("phone")
    @Expose
    private String phone;
    @SerializedName("latitude")
    @Expose
    private Double latitude;
    @SerializedName("longitude")
    @Expose
    private Double longitude;
    @SerializedName("created_date")
    @Expose
    private String createdDate;
    @SerializedName("request_id")
    @Expose
    private String requestId;
    @SerializedName("paired_state")
    @Expose
    private Integer pairedState;

    @Override
    public String toString() {
        return email + ":" + name + ":" + phone + ":" + latitude + ":"
                + longitude + ":" + createdDate + ":" + requestId + ":" + pairedState;
    }

    /**
     * @return The email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email The email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone The phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return The latitude
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude The latitude
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * @return The longitude
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude The longitude
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * @return The createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate The created_date
     */
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return The requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @param requestId The request_id
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * @return The pairedState
     */
    public Integer getPairedState() {
        return pairedState;
    }

    /**
     * @param pairedState The paired_state
     */
    public void setPairedState(Integer pairedState) {
        this.pairedState = pairedState;
    }

}
