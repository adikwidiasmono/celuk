package com.celuk.webservice.api;

import com.celuk.webservice.model.DirectionData;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by adik.widiasmono on 1/10/2017.
 */

public class GoogleDirectionService {
    public static final String BASE_API_URL = "https://maps.googleapis.com";
    private Retrofit retrofit;

    public GoogleDirectionService() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public Call<DirectionData> getDirectionDataGoogleAPI(String key,
                                                         double originLatitude, double originLongitude,
                                                         double destinationLatitude, double destinationLongitude) {
        String origin = originLatitude + "," + originLongitude;
        String destination = destinationLatitude + "," + destinationLongitude;
        return retrofit.create(GoogleDirectionServiceInterface.class).getDirectionDataGoogleAPI(key, origin, destination);
    }
}
