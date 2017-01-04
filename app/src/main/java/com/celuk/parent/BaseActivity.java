package com.celuk.parent;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.celuk.database.model.CelukUser;
import com.celuk.main.LoginActivity;
import com.celuk.main.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.CelukSharedPref;

/**
 * Created by adikwidiasmono on 11/7/16.
 */

public class BaseActivity extends AppCompatActivity {
    protected CelukSharedPref shared;
    // Firebase DB
    protected DatabaseReference mDatabase;
    private ProgressDialog mProgressDialog;
    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    // Firebase User DB
    private Query mUserQuery;
    private ValueEventListener mUserListener;

    // Firebase Pairs DB
//    private Query mPairCallerQuery, mPairReceiverQuery;
//    private ValueEventListener mPairCallerListener, mPairReceiverListener;

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = new CelukSharedPref(getApplicationContext());
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Firebase Auth init
        initUserAuth();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Attach firebase auth and User DB listener
        mAuth.addAuthStateListener(mAuthListener);
        if (mUserQuery != null && mUserListener != null)
            mUserQuery.addValueEventListener(mUserListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Detach firebase auth and User DB listener
        mAuth.removeAuthStateListener(mAuthListener);
        if (mUserQuery != null && mUserListener != null)
            mUserQuery.removeEventListener(mUserListener);

        hideProgressDialog();
    }

    private void initUserAuth() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    shared.setCurrentUser(null);

                    // Kick user to login page
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    // Firebase User DB init
                    initUserWatcher();

                    // Firebase Pairs DB init
//                    initPairsUser();
                }
            }
        };
    }

    private void initUserWatcher() {
        mUserQuery = mDatabase.child("users").child(getUserUid());
        mUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    shared.setCurrentUser(snapshot.getValue(CelukUser.class));

//                    if (celukUser != null && celukUser.getPairedId() != null && celukUser.getPairedId().trim().length() > 0) {
//                        // Attach listener as CALLER
//                        mPairCallerQuery.addListenerForSingleValueEvent(mPairCallerListener);
//                        // Attach listener as RECEIVER
//                        mPairReceiverQuery.addListenerForSingleValueEvent(mPairReceiverListener);
//                    } else {
//                        // Detach listener as CALLER
//                        mPairCallerQuery.removeEventListener(mPairCallerListener);
//                        // Detach listener as RECEIVER
//                        mPairReceiverQuery.removeEventListener(mPairReceiverListener);
//
//                        // Get back user to MainActivity if user isn't paired anymore
//                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        startActivity(intent);
//                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mUserQuery.addValueEventListener(mUserListener);
    }

//    private void initPairsUser() {
//        mPairCallerQuery = mDatabase.child("pairs")
//                .orderByChild("caller").equalTo(getUserUid())
//                .orderByChild("status").equalTo(CelukPair.PAIR_STATUS_ACTIVE);
//        mPairReceiverQuery = mDatabase.child("pairs")
//                .orderByChild("receiver").equalTo(getUserUid())
//                .orderByChild("status").equalTo(CelukPair.PAIR_STATUS_ACTIVE);
//
//        mPairCallerListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    Log.e("CHECK", getLocalClassName() + " : " + MainActivity.class.getCanonicalName() + "|");
//                    if (getLocalClassName().equalsIgnoreCase(MainActivity.class.getCanonicalName())) {
//                        // Redirect to activity as CALLER
//                        Intent intent = new Intent(getApplicationContext(), CallerActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        intent.putExtra("READY", true);
//                        startActivity(intent);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        };
//        mPairReceiverListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    Log.e("CHECK", getLocalClassName() + " : " + MainActivity.class.getCanonicalName() + "|");
//                    if (getLocalClassName().equalsIgnoreCase(MainActivity.class.getCanonicalName())) {
//                        // Redirect to activity as RECEIVER
//                        Intent intent = new Intent(getApplicationContext(), ReceiverActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        intent.putExtra("READY", true);
//                        startActivity(intent);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        };
//    }

    public String getUserUid() {
        return mAuth.getCurrentUser().getUid();
    }

    public void signOut() {
        mAuth.signOut();
    }
}
