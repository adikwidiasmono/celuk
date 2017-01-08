package com.celuk.caller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.celuk.main.LoginActivity;
import com.celuk.main.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CallerReadyFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CallerReadyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CallerReadyFragment extends Fragment {

    private int celukState;
    private String fragmentName;

    private CelukSharedPref shared;
    private DatabaseReference mCelukReference;
    private DatabaseReference celukRequestReference;

    private TextView tvReceiverEmail;
    private Button btCallReceiver;

    private String receiverEmail;

    private OnFragmentInteractionListener mListener;

    public CallerReadyFragment() {
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
    public static CallerReadyFragment newInstance(int celukState, String fragmentName) {
        CallerReadyFragment fragment = new CallerReadyFragment();
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
                CelukRequest request = dataSnapshot.getValue(CelukRequest.class);
                if (request == null)
                    return;

                if (request.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_HISTORY)) {
                    if (mListener != null)
                        mListener.onReceiverStop(CelukState.CELUK_NO_ASSIGNMENT);
                    return;
                }

                if (!request.getStatus().equalsIgnoreCase(CelukRequest.REQUEST_STATUS_ACCEPT))
                    return;

                if (tvReceiverEmail != null) {
                    receiverEmail = request.getReceiver();
                    if (celukState == CelukState.CALLER_CALL_RECEIVER)
                        tvReceiverEmail.setText(receiverEmail + "\n[HASN'T ANSWER YET]");
                    else
                        tvReceiverEmail.setText(request.getReceiver());
                }
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
        View view = inflater.inflate(R.layout.fragment_caller_ready, container, false);

        tvReceiverEmail = (TextView) view.findViewById(R.id.tv_receiver_email);

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

        btCallReceiver = (Button) view.findViewById(R.id.bt_call_receiver);
        if (celukState == CelukState.CALLER_CALL_RECEIVER) {
            if (receiverEmail != null)
                tvReceiverEmail.setText(receiverEmail + "\n[HASN'T ANSWER YET]");
            btCallReceiver.setText("RECALL");
        }
        btCallReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(tvReceiverEmail.getText().toString()))
                    return;

                if (mListener != null) {
                    // Update Caller state. After Caller state updated, it will trigger notification(Caller is calling) in Receiver
                    DatabaseReference mCallerReference = mCelukReference
                            .child("users").child(((CallerActivity) getActivity()).getUserUid());
                    CelukUser celukUser = shared.getCurrentUser();
                    celukUser.setPairedState(CelukState.CALLER_CALL_RECEIVER);

                    mCallerReference.setValue(celukUser);
                    shared.setCurrentUser(celukUser);

                    Toast.makeText(getContext(), "Calling " + receiverEmail + " ...", Toast.LENGTH_SHORT).show();
                    mListener.onCallerCallReceiver(CelukState.CALLER_CALL_RECEIVER);
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
        void onCallerCallReceiver(int nextState);

        void onReceiverStop(int nextState);
    }
}
