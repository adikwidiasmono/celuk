package com.celuk.caller;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.celuk.database.model.CelukRequest;
import com.celuk.database.model.CelukUser;
import com.celuk.main.LoginActivity;
import com.celuk.main.R;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

public class CallerTrackerFragment extends Fragment implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private final static String TAG = CallerTrackerFragment.class.getCanonicalName();
    private final static int LOCATION_REQUEST_PERMISSION = 99;

    private int celukState;
    private String fragmentName;
    private String receiverPhoneNumber;
    private double receiverLatitude = 0, receiverLongitude = 0;

    private CelukSharedPref shared;
    private DatabaseReference mCelukReference;
    private DatabaseReference celukRequestReference;

    private OnFragmentInteractionListener mListener;

    private TextView tvReceiverEmail, tvReceiverState;

    private MapView mMapView;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean isMapReady = false;
    private Marker receiverMarker = null;

    private long UPDATE_INTERVAL = 30 * 1000;  /* 30 secs */
    private long FASTEST_INTERVAL = 10 * 1000; /* 10 sec */

    private CelukUser celukReceiver;

    public CallerTrackerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param celukState   Parameter 1.
     * @param fragmentName Parameter 2.
     * @return A new instance of fragment CallerReadyFragment.
     */
    public static CallerTrackerFragment newInstance(int celukState, String fragmentName) {
        CallerTrackerFragment fragment = new CallerTrackerFragment();
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

                if (!celukRequest.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_ACCEPT))
                    return;

                if (tvReceiverEmail != null)
                    tvReceiverEmail.setText(celukRequest.getReceiver());

                // Get receiver location
                Query qReceiver = mCelukReference
                        .child("users")
                        .orderByChild("email").equalTo(celukRequest.getReceiver());
                qReceiver.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot == null || dataSnapshot.getChildrenCount() != 1)
                            return;

                        DatabaseReference receiverReference = dataSnapshot.getChildren().iterator().next().getRef();
                        celukReceiver = dataSnapshot.getChildren().iterator().next()
                                .getValue(CelukUser.class);
                        if (celukReceiver == null)
                            return;

                        if (tvReceiverState != null) {
                            if (celukState == CelukState.CALLER_CALL_RECEIVER) {
                                tvReceiverState.setText("[Has't RESPONSE]");
                                tvReceiverState.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_red, null));
                            }
                            if (celukState == CelukState.CALLER_WAIT_RECEIVER) {
                                tvReceiverState.setText("[COMING to you]");
                                tvReceiverState.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_green, null));
                            }
                        }

                        if (celukReceiver.getLatitude() == null)
                            receiverLatitude = 0;
                        else
                            receiverLatitude = celukReceiver.getLatitude();
                        if (celukReceiver.getLongitude() == null)
                            receiverLongitude = 0;
                        else
                            receiverLongitude = celukReceiver.getLongitude();
                        Log.e("RECEIVER LOC", "Lat : " + receiverLatitude + ", " + "Long : " + receiverLongitude);

                        receiverPhoneNumber = celukReceiver.getPhone();

                        if (isMapReady) {
                            LatLng latLngReceiver = new LatLng(receiverLatitude, receiverLongitude);

                            if (receiverMarker == null) {
                                MarkerOptions receiverMarkerOpt = new MarkerOptions()
                                        .position(latLngReceiver)
                                        .title(celukReceiver.getEmail())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_ball_pink_16dp));
                                receiverMarker = googleMap.addMarker(receiverMarkerOpt);

                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLngReceiver, 14);
                                googleMap.animateCamera(cameraUpdate);
                            } else {
                                moveAndAnimateMarker(receiverMarker, latLngReceiver, false);
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLngReceiver);
                                googleMap.animateCamera(cameraUpdate);
                            }
                        }
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
        View view = inflater.inflate(R.layout.fragment_caller_tracker, container, false);

        tvReceiverEmail = (TextView) view.findViewById(R.id.tv_receiver_email);
        tvReceiverState = (TextView) view.findViewById(R.id.tv_receiver_called_status);

        FloatingActionButton fabCallReceiverPhone = (FloatingActionButton) view.findViewById(R.id.fab_call_receiver_phone);
        fabCallReceiverPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:" + receiverPhoneNumber);
                startActivity(new Intent(Intent.ACTION_DIAL, number));
            }
        });

        FloatingActionButton fabStopCeluk = (FloatingActionButton) view.findViewById(R.id.fab_stop_celuk);
        fabStopCeluk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear all state for both user and set request data as history
                if (celukReceiver != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("End CELUK Pairing")
                            .setMessage("Are you sure want to end pairing with " + celukReceiver.getEmail())
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mListener != null) {
                                        mListener.onEndCELUKPairing(CelukState.CELUK_NO_ASSIGNMENT);

                                        // Update active request data to history
                                        celukRequestReference.child("status").setValue(CelukRequest.REQUEST_STATUS_HISTORY);
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });

        TextView tvSignOut = (TextView) view.findViewById(R.id.tv_sign_out);
        tvSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CallerActivity) getActivity()).signOut();
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
        mMapView = (MapView) view.findViewById(R.id.mv_receiver);
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

//        // For dropping a marker at a point on the Map
//        LatLng receiverLoc = new LatLng(receiverLatitude, receiverLongitude);
//        googleMap.addMarker(new MarkerOptions().position(receiverLoc).title("Marker Title").snippet("Marker Description"));
//
//        // For zooming automatically to the location of the marker
//        CameraPosition cameraPosition = new CameraPosition.Builder().target(receiverLoc).zoom(12).build();
//        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mListener != null) {
            mListener.onChangeCallerLocation(location.getLatitude(), location.getLongitude());
        }
    }

    public void moveAndAnimateMarker(final Marker marker, final LatLng toPosition,
                                     final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = googleMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
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
        void onChangeCallerLocation(double latitude, double longitude);

        void onEndCELUKPairing(int celukState);
    }
}
