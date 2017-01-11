package com.celuk.receiver;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.celuk.database.model.CelukRequest;
import com.celuk.database.model.CelukUser;
import com.celuk.main.LoginActivity;
import com.celuk.main.R;
import com.celuk.webservice.api.GoogleDirectionService;
import com.celuk.webservice.model.DirectionData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

import java.net.HttpURLConnection;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReceiverTrackerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReceiverTrackerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReceiverTrackerFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private final static String TAG = ReceiverTrackerFragment.class.getCanonicalName();
    private final static int LOCATION_REQUEST_PERMISSION = 99;
    boolean isFirstDisplay = true;
    private int celukState;
    private String fragmentName;
    private String callerPhoneNumber;
    private double callerLatitude = 0, callerLongitude = 0;
    private CelukSharedPref shared;
    private DatabaseReference mCelukReference;
    private DatabaseReference celukRequestReference;
    private OnFragmentInteractionListener mListener;
    private TextView tvCallerEmail;
    private MapView mMapView;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean isMapReady = false;
    private boolean hasDrawDirection = false;
    private long UPDATE_INTERVAL = 30 * 1000;  /* 30 secs */
    private long FASTEST_INTERVAL = 10 * 1000; /* 10 sec */

    public ReceiverTrackerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param celukState   Parameter 1.
     * @param fragmentName Parameter 2.
     * @return A new instance of fragment ReceiverTrackerFragment.
     */
    public static ReceiverTrackerFragment newInstance(int celukState, String fragmentName) {
        ReceiverTrackerFragment fragment = new ReceiverTrackerFragment();
        fragment.celukState = celukState;
        fragment.fragmentName = fragmentName;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = new CelukSharedPref(getContext());
        mCelukReference = FirebaseDatabase.getInstance().getReference();

        celukRequestReference = mCelukReference
                .child("requests")
                .child(shared.getCurrentUser().getRequestId());
        celukRequestReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                CelukRequest celukRequest = dataSnapshot.getValue(CelukRequest.class);
                if (celukRequest == null)
                    return;

                if (celukRequest.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_HISTORY)) {
                    if (mListener != null)
                        mListener.onEndCELUKPairing(CelukState.CELUK_NO_ASSIGNMENT);
                    return;
                }

                if (!celukRequest.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_ACCEPT))
                    return;

                if (tvCallerEmail != null)
                    tvCallerEmail.setText(celukRequest.getCaller());

                // Get caller location
                Query qCaller = mCelukReference
                        .child("users")
                        .orderByChild("email").equalTo(celukRequest.getCaller());
                qCaller.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot == null || dataSnapshot.getChildrenCount() != 1)
                            return;

                        final DatabaseReference callerRef = dataSnapshot.getChildren().iterator().next().getRef();
                        CelukUser celukCaller = dataSnapshot.getChildren().iterator().next()
                                .getValue(CelukUser.class);
                        if (celukCaller == null)
                            return;

                        if (celukCaller.getLatitude() == null)
                            callerLatitude = 0;
                        else
                            callerLatitude = celukCaller.getLatitude();
                        if (celukCaller.getLongitude() == null)
                            callerLongitude = 0;
                        else
                            callerLongitude = celukCaller.getLongitude();
//                        Log.e("CALLER LOC", "Lat : " + callerLatitude + ", " + "Long : " + callerLongitude);

                        callerPhoneNumber = celukCaller.getPhone();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        setupGoogleApiClient();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_receiver_tracker, container, false);

        tvCallerEmail = (TextView) view.findViewById(R.id.tv_caller_email);
        FloatingActionButton fabCallCallerPhone = (FloatingActionButton) view.findViewById(R.id.fab_call_caller_phone);
        fabCallCallerPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:" + callerPhoneNumber);
                startActivity(new Intent(Intent.ACTION_DIAL, number));
            }
        });

        FloatingActionButton fabNavigateReceiver = (FloatingActionButton) view.findViewById(R.id.fab_navigate_receiver);
        fabNavigateReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callerLatitude != 0 && callerLongitude != 0) {
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?daddr=" + callerLatitude + "," + callerLongitude));
                    startActivity(intent);
                }
            }
        });

        TextView tvSignOut = (TextView) view.findViewById(R.id.tv_sign_out);
        tvSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ReceiverActivity) getActivity()).signOut();
                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        // Define google map here
        setupMap(view, savedInstanceState);

        return view;
    }

    private void setupGoogleApiClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void setupMap(View view, Bundle savedInstanceState) {
        mMapView = (MapView) view.findViewById(R.id.mv_caller);
        mMapView.onCreate(savedInstanceState);

        // Get the button view
        View locationButton = ((View) mMapView.findViewById(Integer.valueOf("1")).getParent()).findViewById(Integer.valueOf("2"));
        // and next place it on bottom left
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        layoutParams.setMargins(10, 0, 0, 92);
        locationButton.setLayoutParams(layoutParams);

//        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            LOCATION_REQUEST_PERMISSION);
                }
                return;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        // only stop if it's connected, otherwise we crash
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected())
                // Disconnecting the client invalidates it.
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Get last known recent location.
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_PERMISSION);
            return;
        }
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // Note that this can be NULL if last location isn't already known.
        if (mCurrentLocation != null) {
            // Print current location if not null
            Log.d("DEBUG", "current location: " + mCurrentLocation.toString());
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        }
        // Begin polling for new location updates.
        startLocationUpdates();
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_PERMISSION);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(getContext(), "Disconnected from GCM Server", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(getContext(), "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        googleMap = gMap;

        // Check location permission
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_PERMISSION);
            return;
        }
        googleMap.setMyLocationEnabled(true);
        isMapReady = true;

//                // For dropping a marker at a point on the Map
//                LatLng callerLoc = new LatLng(callerLatitude, callerLongitude);
//                googleMap.addMarker(new MarkerOptions().position(callerLoc).title("Marker Title").snippet("Marker Description"));
//
//                // For zooming automatically to the location of the marker
//                CameraPosition cameraPosition = new CameraPosition.Builder().target(callerLoc).zoom(12).build();
//                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLngReceiver = new LatLng(location.getLatitude(), location.getLongitude());
        if (mListener != null) {
            mListener.onChangeReceiverLocation(location.getLatitude(), location.getLongitude());
        }

        if (isMapReady) {
            if ((callerLatitude != 0) && (callerLongitude != 0)) {
                LatLng latLngCaller = new LatLng(callerLatitude, callerLongitude);
                googleMap.addMarker(new MarkerOptions()
                        .position(latLngCaller)
                        .title(tvCallerEmail.getText().toString())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_ball_pink_16dp)));

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLngCaller);
                googleMap.animateCamera(cameraUpdate);
            }

            if ((location.getAccuracy() < 30) && !hasDrawDirection && (callerLatitude != 0) && (callerLongitude != 0)) {
                drawDirectionOnMap(getString(R.string.google_maps_key),
                        location.getLatitude(), location.getLongitude(), callerLatitude, callerLongitude);
            }

            if (isFirstDisplay) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLngReceiver, 14);
                googleMap.animateCamera(cameraUpdate);
                isFirstDisplay = false;
            }
        }
    }

    private void drawDirectionOnMap(String key,
                                    double originLatitude, double originLongitude,
                                    double destinationLatitude, double destinationLongitude) {
        // Draw a direction here
        Call<DirectionData> callDirection = new GoogleDirectionService().getDirectionDataGoogleAPI(key,
                originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        callDirection.enqueue(new Callback<DirectionData>() {
            @Override
            public void onResponse(Call<DirectionData> call, Response<DirectionData> response) {
                if (response.code() == HttpURLConnection.HTTP_OK) {
                    if (response.body().getStatus().equalsIgnoreCase("OK")) {
                        try {
                            List<LatLng> listPoint = PolyUtil.decode(response.body().getRoutes().get(0).getOverviewPolyline().getPoints());
                            // Setup polyline
                            PolylineOptions lineOptions = new PolylineOptions();
                            lineOptions.addAll(listPoint);
                            lineOptions.width(10);
                            lineOptions.color(ContextCompat.getColor(getContext(), R.color.c_green));

                            googleMap.addPolyline(lineOptions);
                            hasDrawDirection = true;
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                Toast.makeText(getContext(), "Failed request direction to google API", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<DirectionData> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(getContext(), "Failed request direction to google API", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onChangeReceiverLocation(double latitude, double longitude);

        void onEndCELUKPairing(int celukState);
    }
}
