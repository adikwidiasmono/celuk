package com.celuk.receiver;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.celuk.database.model.CelukRequest;
import com.celuk.database.model.CelukUser;
import com.celuk.main.LoginActivity;
import com.celuk.main.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReceiverReadyFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReceiverReadyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReceiverReadyFragment extends Fragment {
    private final static String TAG = ReceiverReadyFragment.class.getCanonicalName();

    private int celukState;
    private String fragmentName;
    private CelukUser celukCaller;

    private CelukSharedPref shared;
    private DatabaseReference mCelukReference;
    private DatabaseReference celukRequestReference;

    private TextView tvCallerEmail;

    private OnFragmentInteractionListener mListener;

    public ReceiverReadyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param celukState   Parameter 1.
     * @param fragmentName Parameter 2.
     * @return A new instance of fragment ReceiverReadyFragment.
     */
    public static ReceiverReadyFragment newInstance(int celukState, String fragmentName) {
        ReceiverReadyFragment fragment = new ReceiverReadyFragment();
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
                final CelukRequest request = dataSnapshot.getValue(CelukRequest.class);
                if (request == null)
                    return;

                if (!(request.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_ACCEPT) ||
                        request.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_HISTORY)))
                    return;

                if (tvCallerEmail != null)
                    tvCallerEmail.setText(request.getCaller());

                final Query qCaller = mCelukReference
                        .child("users")
                        .orderByChild("email").equalTo(request.getCaller());
                qCaller.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot == null || dataSnapshot.getChildrenCount() != 1)
                            return;

                        final DatabaseReference callerRef = dataSnapshot.getChildren().iterator().next().getRef();
                        celukCaller = dataSnapshot.getChildren().iterator().next()
                                .getValue(CelukUser.class);
                        if (celukCaller == null)
                            return;

                        if (request.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_HISTORY)) {
                            Toast.makeText(getContext(), "You have stopped as RECEIVER for " + celukCaller.getEmail(), Toast.LENGTH_SHORT).show();

                            // Update Celuk Caller State if Celuk Receiver do Stop As Receiver
                            celukCaller.setPairedState(CelukState.CELUK_NO_ASSIGNMENT);
                            celukCaller.setRequestId(null);
                            callerRef.setValue(celukCaller);

                            // Update receiver to duty free
                            mListener.onStopAsReceiver(CelukState.CELUK_NO_ASSIGNMENT, null);
                            return;
                        }

                        if (celukCaller.getPairedState() == CelukState.CALLER_CALL_RECEIVER)
                            new AlertDialog.Builder(getContext())
                                    .setTitle("GET A CALL")
                                    .setMessage("Incoming call from " + celukCaller.getEmail())
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (mListener != null) {
                                                celukCaller.setPairedState(CelukState.CALLER_WAIT_RECEIVER);
                                                callerRef.setValue(celukCaller);

                                                mListener.onCallerAcceptCall(CelukState.RECEIVER_ACCEPT_CALL, shared.getCurrentUser().getRequestId());
                                            }
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setCancelable(false)
                                    .show();
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
        View view = inflater.inflate(R.layout.fragment_receiver_ready, container, false);

        tvCallerEmail = (TextView) view.findViewById(R.id.tv_caller_email);

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

        TextView tvStopAsReceiver = (TextView) view.findViewById(R.id.tv_stop_as_receiver);
        tvStopAsReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (celukRequestReference != null && mListener != null) {
                    // Update Celuk Request as a history
                    celukRequestReference.child("status").setValue(CelukRequest.REQUEST_STATUS_HISTORY);
                }
            }
        });

        return view;
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
        void onCallerAcceptCall(int nextState, String requestId);

        void onStopAsReceiver(int nextState, String requestId);
    }
}
