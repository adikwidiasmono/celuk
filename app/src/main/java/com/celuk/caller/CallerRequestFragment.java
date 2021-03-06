package com.celuk.caller;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.celuk.database.model.CelukRequest;
import com.celuk.database.model.CelukUser;
import com.celuk.database.viewholder.CelukCallerRequestViewHolder;
import com.celuk.main.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.AppDateUtils;
import com.utils.AppUtils;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

public class CallerRequestFragment extends Fragment {
    private final static String TAG = CallerRequestFragment.class.getCanonicalName();

    private int celukState;
    private String fragmentName;

    private DatabaseReference mCelukReference;
    private Query qCallerRequest;

    private CelukSharedPref shared;

    private TextInputLayout tilReceiverEmail;
    private TextInputEditText etReceiverEmail;
    private FirebaseRecyclerAdapter<CelukRequest, CelukCallerRequestViewHolder> mAdapter;
    private RecyclerView mRecycler;
    private TextView tvNoRequest;

    private Query qCelukUser;
    private ValueEventListener qCelukUserListener;
    private ValueEventListener qCallerRequestListener;

    private OnFragmentInteractionListener mListener;

    public CallerRequestFragment() {
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
    public static CallerRequestFragment newInstance(int celukState, String fragmentName) {
        CallerRequestFragment fragment = new CallerRequestFragment();
        fragment.celukState = celukState;
        fragment.fragmentName = fragmentName;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCelukReference = FirebaseDatabase.getInstance().getReference();
        shared = new CelukSharedPref(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_caller_request, container, false);

        tilReceiverEmail = (TextInputLayout) view.findViewById(R.id.til_receiver_email);
        etReceiverEmail = (TextInputEditText) view.findViewById(R.id.et_receiver_email);
        tvNoRequest = (TextView) view.findViewById(R.id.tv_no_request);

        // Set up FirebaseRecyclerAdapter with the Query
        qCallerRequest = mCelukReference.child("requests")
                .orderByChild("caller")
                .equalTo(shared.getCurrentUser().getEmail());

        initRecyclerView(view);

        // Add listener to change fragment to ready fragment if receiver accept the request
        qCallerRequest.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getChildrenCount() > 0) {
                    tvNoRequest.setVisibility(View.GONE);
                    for (DataSnapshot snapChild : dataSnapshot.getChildren()) {
                        CelukRequest req = snapChild.getValue(CelukRequest.class);
                        if (req == null)
                            return;

                        if (req.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_ACCEPT)) {
                            if (mListener != null)
                                mListener.onRequestAccepted(CelukState.CALLER_READY, snapChild.getKey());
                        }
                    }
                } else {
                    tvNoRequest.setText(getResources().getString(R.string.no_request));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Button btRequestReceiver = (Button) view.findViewById(R.id.bt_req_receiver);
        btRequestReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String receiverEmail = etReceiverEmail.getText().toString();

                if (!validateForm(receiverEmail)) {
                    return;
                }

                checkReceiverExist(shared.getCurrentUser().getEmail(), receiverEmail);
            }
        });

        return view;
    }

    private void initRecyclerView(View v) {
        mRecycler = (RecyclerView) v.findViewById(R.id.rv_receiver_req_list);
        mRecycler.setHasFixedSize(true);

        // Set up Layout Manager, reverse layout
        LinearLayoutManager mManager = new LinearLayoutManager(getContext());
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(mManager);

        createAdapter(qCallerRequest);

        mRecycler.setAdapter(mAdapter);
    }

    public void createAdapter(Query query) {
        mAdapter = new FirebaseRecyclerAdapter<CelukRequest, CelukCallerRequestViewHolder>(
                CelukRequest.class, R.layout.item_celuk_receiver_request,
                CelukCallerRequestViewHolder.class, query) {
            @Override
            protected void populateViewHolder(final CelukCallerRequestViewHolder viewHolder,
                                              final CelukRequest model, final int position) {
                final DatabaseReference postRef = getRef(position);

                // Set click listener for the whole post view
                final String postKey = postRef.getKey();
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Launch PostDetailActivity
//                        Intent intent = new Intent(getActivity(), PostDetailActivity.class);
//                        intent.putExtra(PostDetailActivity.EXTRA_POST_KEY, postKey);
//                        startActivity(intent);
                    }
                });

                Log.e(TAG, "Content : " + model.toString());
//                checkContent(postKey);
                viewHolder.bindToHolder(model);
            }
        };
    }

    private boolean validateForm(String email) {
        tilReceiverEmail.setError(null);

        boolean isValid = true;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            tilReceiverEmail.setError(getString(R.string.error_field_required));
            focusView = etReceiverEmail;
            isValid = false;
        } else if (!AppUtils.isEmailValid(email)) {
            tilReceiverEmail.setError(getString(R.string.error_invalid_email));
            focusView = etReceiverEmail;
            isValid = false;
        }

        if (!isValid) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            Toast.makeText(getContext(), "Sending request to " + email, Toast.LENGTH_SHORT).show();
            etReceiverEmail.setText(null);
        }

        return isValid;
    }

    private void checkReceiverExist(final String callerEmail, final String receiverEmail) {
        qCelukUser = mCelukReference.child("users")
                .orderByChild("email").equalTo(receiverEmail);
        qCelukUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                qCelukUser.removeEventListener(qCelukUserListener);

                if (dataSnapshot == null || dataSnapshot.getChildrenCount() != 1) {
                    Log.e(TAG, "SATU");
                    tilReceiverEmail.setError("User doesn't exist, try another user");
                    return;
                }

                CelukUser celukUser = dataSnapshot.getChildren().iterator().next()
                        .getValue(CelukUser.class);
                if (celukUser != null) {
                    Log.e(TAG, celukUser.toString());
                    checkIsRequestExsist(callerEmail, receiverEmail);
                } else {
                    Log.e(TAG, "DUA");
                    tilReceiverEmail.setError("User doesn't exist, try another user");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        qCelukUser.addValueEventListener(qCelukUserListener);
    }

    private void checkIsRequestExsist(final String callerEmail, final String receiverEmail) {
        qCallerRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getChildrenCount() > 0) {
                    qCallerRequest.removeEventListener(qCallerRequestListener);

                    for (DataSnapshot snapChild : dataSnapshot.getChildren()) {
                        CelukRequest req = snapChild.getValue(CelukRequest.class);
                        if (req == null)
                            break;

                        // There is active state for this user
                        if (req.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_ACCEPT)) {
                            Log.e(TAG, "TIGA");
                            tilReceiverEmail.setError("You have an active pair");
                            return;
                        }
                        if (req.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_PENDING) &&
                                req.getCaller().equalsIgnoreCase(callerEmail) &&
                                req.getReceiver().equalsIgnoreCase(receiverEmail)) {
                            Log.e(TAG, "EMPAT");
                            tilReceiverEmail.setError("Cannot send request to same user");
                            return;
                        }
                    }
                }

                // Saving new Celuk Request
                saveCelukRequest(callerEmail, receiverEmail);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        qCallerRequest.addValueEventListener(qCallerRequestListener);
    }

    private void saveCelukRequest(String callerEmail, String receiverEmail) {
        CelukRequest celukRequest = new CelukRequest();
        celukRequest.setCaller(callerEmail);
        celukRequest.setReceiver(receiverEmail);
        celukRequest.setRequestedDate(AppDateUtils.getCurrentDate(AppDateUtils.APP_DATE_PATTERN));
        celukRequest.setStatus(CelukRequest.REQUEST_STATUS_PENDING);
        mCelukReference.child("requests").push().setValue(celukRequest);

        Toast.makeText(getContext(), "Request has been sent to " + receiverEmail, Toast.LENGTH_SHORT).show();
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

    public interface OnFragmentInteractionListener {
        void onRequestAccepted(int nextState, String requestId);
    }

}
