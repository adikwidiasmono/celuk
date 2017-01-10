package com.celuk.webservice.api;

import com.celuk.webservice.model.DirectionData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by adik.widiasmono on 1/10/2017.
 */

public interface GoogleDirectionServiceInterface {
    @GET("/maps/api/directions/json")
    Call<DirectionData> getDirectionDataGoogleAPI(
            @Query("key") String key,
            @Query("origin") String origin,
            @Query("destination") String destination
    );
}
