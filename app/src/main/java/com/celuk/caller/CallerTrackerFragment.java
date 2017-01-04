package com.celuk.caller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.celuk.database.model.CelukPair;
import com.celuk.database.model.CelukUser;
import com.celuk.main.R;
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
import com.utils.CelukState;

public class CallerTrackerFragment extends Fragment {
    private static final String CELUK_STATE = "celukState";
    private static final String FRAGMENT_NAME = "fragmentName";

    private int celukState;
    private String fragmentName;
    private String receiverPhoneNumber;
    private double latitude, longitude;

    private CelukSharedPref shared;
    private DatabaseReference mCelukReference;

    private TextView tvReceiverEmail, tvReceiverState;
    private FloatingActionButton fabCallReceiverPhone;

    private MapView mMapView;
    private GoogleMap googleMap;

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
        Bundle args = new Bundle();
        args.putInt(CELUK_STATE, celukState);
        args.putString(FRAGMENT_NAME, fragmentName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            celukState = getArguments().getInt(CELUK_STATE);
            fragmentName = getArguments().getString(FRAGMENT_NAME);
        }

        shared = new CelukSharedPref(getContext());
        mCelukReference = FirebaseDatabase.getInstance().getReference();

        final Query qPair = mCelukReference
                .child("pairs")
                .orderByChild("caller").equalTo(shared.getCurrentUser().getEmail());
        qPair.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                CelukPair pair = dataSnapshot.getValue(CelukPair.class);
                if (pair == null)
                    return;

                if (pair.getStatus().equalsIgnoreCase(CelukPair.PAIR_STATUS_INACTIVE))
                    return;

                if (tvReceiverEmail != null)
                    tvReceiverEmail.setText(pair.getReceiver());

                // Get receiver location
                Query qReceiver = mCelukReference
                        .child("users")
                        .orderByChild("email").equalTo(pair.getReceiver());
                qReceiver.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        CelukUser celukReceiver = dataSnapshot.getValue(CelukUser.class);
                        if (celukReceiver == null)
                            return;

                        if (celukReceiver.getLatitude() == null || celukReceiver.getLongitude() == null)
                            return;

                        Log.e("RECEIVER LOC", "Lat : " + celukReceiver.getLatitude() + ", " + "Long : " + celukReceiver.getLongitude());
                        latitude = celukReceiver.getLatitude();
                        longitude = celukReceiver.getLongitude();

                        receiverPhoneNumber = celukReceiver.getPhone();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_caller_tracker, container, false);

        tvReceiverEmail = (TextView) view.findViewById(R.id.tv_receiver_email);
        tvReceiverState = (TextView) view.findViewById(R.id.tv_receiver_called_status);
        if (celukState == CelukState.CALLER_CALL_RECEIVER)
            tvReceiverState.setText("[Has't RESPONSE]");
        if (celukState == CelukState.CALLER_WAIT_RECEIVER) {
            tvReceiverState.setText("[COMING to you]");
            tvReceiverState.setTextColor(ResourcesCompat.getColor(getResources(), R.color.c_green, null));
        }

        fabCallReceiverPhone = (FloatingActionButton) view.findViewById(R.id.fab_call_receiver_phone);
        fabCallReceiverPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri number = Uri.parse("tel:" + receiverPhoneNumber);
                startActivity(new Intent(Intent.ACTION_DIAL, number));
            }
        });

        // Define google map here
        setupMap(view, savedInstanceState);

        return view;
    }

    private void setupMap(View view, Bundle savedInstanceState) {
        mMapView = (MapView) view.findViewById(R.id.mv_receiver);
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
                    mMap.setMyLocationEnabled(true);
                } else {
                    // Show rationale and request permission.
                }

                // For dropping a marker at a point on the Map
                LatLng receiverLoc = new LatLng(latitude, longitude);
                googleMap.addMarker(new MarkerOptions().position(receiverLoc).title("Marker Title").snippet("Marker Description"));

                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder().target(receiverLoc).zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
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

}
