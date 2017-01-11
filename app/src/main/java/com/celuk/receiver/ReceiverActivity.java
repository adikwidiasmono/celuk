package com.celuk.receiver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.celuk.database.model.CelukUser;
import com.celuk.main.MainActivity;
import com.celuk.main.R;
import com.celuk.parent.BaseActivity;
import com.utils.CelukState;

public class ReceiverActivity extends BaseActivity implements
        ReceiverRequestFragment.OnFragmentInteractionListener,
        ReceiverReadyFragment.OnFragmentInteractionListener,
        ReceiverTrackerFragment.OnFragmentInteractionListener {

    private boolean isReady;
    private boolean isResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(!isReady);

        isReady = getIntent().getBooleanExtra("READY", false);

        setupFragment(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = getIntent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isResume)
            updateActiveFragment(getActiveFragment());
        isResume = true;
    }

    @Override
    public void onBackPressed() {
        if (isReady) {
            moveTaskToBack(true);
        } else {
            Intent intent = getIntent();
            intent.setClass(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupFragment(Bundle savedInstanceState) {
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fl_receiver) != null) {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fl_receiver, getActiveFragment())
                    .commit();
        }
    }

    private void updateActiveFragment(Fragment newFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fl_receiver, newFragment)
                .addToBackStack(null)
                // Commit the transaction
                .commit();
    }

    private Fragment getActiveFragment() {
        Fragment activeFragment = ReceiverRequestFragment.newInstance(CelukState.CELUK_NO_ASSIGNMENT, "Receiver Get Request");

        switch (shared.getCurrentUser().getPairedState()) {
            case CelukState.CELUK_NO_ASSIGNMENT:
                isReady = false;
                activeFragment = ReceiverRequestFragment.newInstance(CelukState.CELUK_NO_ASSIGNMENT, "Receiver Get Request");
                break;
            case CelukState.RECEIVER_READY:
                isReady = true;
                activeFragment = ReceiverReadyFragment.newInstance(CelukState.RECEIVER_READY, "Receiver Ready");
                break;
//            case CelukState.RECEIVER_GET_CALL:
//                isReady = true;
//                activeFragment = ReceiverTrackerFragment.newInstance(CelukState.RECEIVER_GET_CALL, "Receiver Get Call");
//                break;
            case CelukState.RECEIVER_ACCEPT_CALL:
                isReady = true;
                activeFragment = ReceiverTrackerFragment.newInstance(CelukState.RECEIVER_ACCEPT_CALL, "Receiver Accept Call");
                break;
            default:
                break;
        }

        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        activeFragment.setArguments(getIntent().getExtras());

        return activeFragment;
    }

    private void updateReceiverState(int state, String requestId) {
        CelukUser user = shared.getCurrentUser();
        user.setPairedState(state);
        user.setRequestId(requestId);
        shared.setCurrentUser(user);

        mDatabase.child("users")
                .child(getUserUid())
                .setValue(user);

        updateActiveFragment(getActiveFragment());
    }

    @Override
    public void onReceiverAcceptRequest(int nextState, String requestId) {
        updateReceiverState(nextState, requestId);
    }

    @Override
    public void onReceiverAcceptCall(int nextState, String requestId) {
        updateReceiverState(nextState, requestId);
    }

    @Override
    public void onStopAsReceiver(int nextState) {
        updateReceiverState(nextState, null);
    }

    @Override
    public void onEndCELUKPairing(int celukState) {
        updateReceiverState(celukState, null);
    }

    @Override
    public void onChangeReceiverLocation(double latitude, double longitude) {
        CelukUser user = shared.getCurrentUser();
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        shared.setCurrentUser(user);

        mDatabase.child("users")
                .child(getUserUid())
                .setValue(user);
    }
}
