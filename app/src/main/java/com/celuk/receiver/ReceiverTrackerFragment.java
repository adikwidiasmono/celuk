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
import android.widget.TextView;
import android.widget.Toast;

import com.celuk.database.model.CelukPair;
import com.celuk.database.model.CelukUser;
import com.celuk.main.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.CelukSharedPref;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReceiverTrackerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReceiverTrackerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReceiverTrackerFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private final static String TAG = ReceiverTrackerFragment.class.getCanonicalName();

    private int celukState;
    private String fragmentName;
    private String callerPhoneNumber;
    private double callerLatitude, callerLongitude, receiverLatitude, receiverLongitude;

    private CelukSharedPref shared;
    private DatabaseReference mCelukReference;

    private OnFragmentInteractionListener mListener;

    private TextView tvCallerEmail;
    private FloatingActionButton fabCallCallerPhone;
    private MapView mMapView;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

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

        final Query qPair = mCelukReference
                .child("pairs")
                .orderByChild("receiver").equalTo(shared.getCurrentUser().getEmail());
        qPair.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                CelukPair pair = dataSnapshot.getValue(CelukPair.class);
                if (pair == null)
                    return;

                if (pair.getStatus().equalsIgnoreCase(CelukPair.PAIR_STATUS_INACTIVE))
                    return;

                if (tvCallerEmail != null)
                    tvCallerEmail.setText(pair.getCaller());

                // Get caller location
                Query qReceiver = mCelukReference
                        .child("users")
                        .orderByChild("email").equalTo(pair.getCaller());
                qReceiver.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        CelukUser celukCaller = dataSnapshot.getValue(CelukUser.class);
                        if (celukCaller == null)
                            return;

                        if (celukCaller.getLatitude() == null || celukCaller.getLongitude() == null)
                            return;

                        Log.e("CALLER LOC", "Lat : " + celukCaller.getLatitude() + ", " + "Long : " + celukCaller.getLongitude());
                        if (celukCaller.getLatitude() == null)
                            callerLatitude = 0;
                        else
                            callerLatitude = celukCaller.getLatitude();

                        if (celukCaller.getLongitude() == null)
                            callerLongitude = 0;
                        else
                            callerLongitude = celukCaller.getLongitude();

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
        fabCallCallerPhone = (FloatingActionButton) view.findViewById(R.id.fab_call_caller_phone);
        fabCallCallerPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:" + callerPhoneNumber);
                startActivity(new Intent(Intent.ACTION_DIAL, number));
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

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                // For showing a move to my location button
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                } else {
                    // Show rationale and request permission.
                }

                // For dropping a marker at a point on the Map
                LatLng callerLoc = new LatLng(callerLatitude, callerLongitude);
                googleMap.addMarker(new MarkerOptions().position(callerLoc).title("Marker Title").snippet("Marker Description"));

                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder().target(callerLoc).zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        // Disconnecting the client invalidates it.
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        // only stop if it's connected, otherwise we crash
        if (mGoogleApiClient != null) {
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
    public void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        // You can now create a LatLng Object for use with maps
//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (mListener != null) {
            mListener.onChangeReceiverLocation(location.getLatitude(), location.getLongitude());
        }
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
    }
}
