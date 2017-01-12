package com.celuk.receiver;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.celuk.database.model.CelukRequest;
import com.celuk.database.viewholder.CelukReceiverRequestViewHolder;
import com.celuk.main.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.AppDateUtils;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReceiverRequestFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReceiverRequestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReceiverRequestFragment extends Fragment {
    private final static String TAG = ReceiverRequestFragment.class.getCanonicalName();

    private int celukState;
    private String fragmentName;

    private DatabaseReference mCelukReference;

    private CelukSharedPref shared;

    private FirebaseRecyclerAdapter<CelukRequest, CelukReceiverRequestViewHolder> mAdapter;
    private RecyclerView mRecycler;
    private TextView tvNoRequest;

    private OnFragmentInteractionListener mListener;

    public ReceiverRequestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param celukState   Parameter 1.
     * @param fragmentName Parameter 2.
     * @return A new instance of fragment ReceiverRequestFragment.
     */
    public static ReceiverRequestFragment newInstance(int celukState, String fragmentName) {
        ReceiverRequestFragment fragment = new ReceiverRequestFragment();
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
        View view = inflater.inflate(R.layout.fragment_receiver_request, container, false);
        tvNoRequest = (TextView) view.findViewById(R.id.tv_no_request);
        initRecyclerView(view);

        return view;
    }

    private void initRecyclerView(View v) {
        mRecycler = (RecyclerView) v.findViewById(R.id.rv_caller_req_list);
        mRecycler.setHasFixedSize(true);

        // Set up Layout Manager, reverse layout
        LinearLayoutManager mManager = new LinearLayoutManager(getContext());
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(mManager);

        // Set up FirebaseRecyclerAdapter with the Query
        Query postsQuery = mCelukReference
                .child("requests")
                .orderByChild("receiver")
                .equalTo(shared.getCurrentUser().getEmail());
        createAdapter(postsQuery);
        postsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null || dataSnapshot.getChildrenCount() < 1) {
                    tvNoRequest.setText(getResources().getString(R.string.no_request));
                } else {
                    tvNoRequest.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRecycler.setAdapter(mAdapter);
    }

    public void createAdapter(Query query) {
        mAdapter = new FirebaseRecyclerAdapter<CelukRequest, CelukReceiverRequestViewHolder>(
                CelukRequest.class, R.layout.item_celuk_caller_request,
                CelukReceiverRequestViewHolder.class, query) {
            @Override
            protected void populateViewHolder(final CelukReceiverRequestViewHolder viewHolder,
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

                viewHolder.bindToHolder(model);
                viewHolder.ivReqAccept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Update Celuk Request Data
                        model.setResponseDate(AppDateUtils.getCurrentDate(AppDateUtils.APP_DATE_PATTERN));
                        model.setStatus(CelukRequest.REQUEST_STATUS_ACCEPT);
                        postRef.setValue(model);

                        // Update Celuk Receiver Data
                        if (mListener != null) {
                            mListener.onReceiverAcceptRequest(CelukState.RECEIVER_READY, postKey);
                        }
                    }
                });
                viewHolder.ivReqReject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        model.setResponseDate(AppDateUtils.getCurrentDate(AppDateUtils.APP_DATE_PATTERN));
                        model.setStatus(CelukRequest.REQUEST_STATUS_REJECT);
                        postRef.setValue(model);
                    }
                });
            }
        };
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
        void onReceiverAcceptRequest(int nextState, String requestId);
    }
}
