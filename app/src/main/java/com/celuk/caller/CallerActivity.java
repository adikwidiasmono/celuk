package com.celuk.caller;

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

public class CallerActivity extends BaseActivity implements
        CallerRequestFragment.OnFragmentInteractionListener,
        CallerReadyFragment.OnFragmentInteractionListener,
        CallerTrackerFragment.OnFragmentInteractionListener {

    private boolean isReady;
    private boolean isResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        if (findViewById(R.id.fl_caller) != null) {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fl_caller, getActiveFragment())
                    .commit();
        }
    }

    private void updateActiveFragment(Fragment newFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fl_caller, newFragment)
                .addToBackStack(null)
                // Commit the transaction
                .commit();
    }

    private Fragment getActiveFragment() {
        Fragment activeFragment = CallerRequestFragment.newInstance(CelukState.CELUK_NO_ASSIGNMENT, "Caller Request Receiver");

        switch (shared.getCurrentUser().getPairedState()) {
            case CelukState.CELUK_NO_ASSIGNMENT:
                isReady = false;
                activeFragment = CallerRequestFragment.newInstance(CelukState.CELUK_NO_ASSIGNMENT, "Caller Request Receiver");
                break;
            case CelukState.CALLER_READY:
                isReady = true;
                activeFragment = CallerReadyFragment.newInstance(CelukState.CALLER_READY, "Caller Ready");
                break;
            case CelukState.CALLER_CALL_RECEIVER:
                isReady = true;
                activeFragment = CallerReadyFragment.newInstance(CelukState.CALLER_CALL_RECEIVER, "Caller Call Receiver");
                break;
            case CelukState.CALLER_WAIT_RECEIVER:
                isReady = true;
                activeFragment = CallerTrackerFragment.newInstance(CelukState.CALLER_WAIT_RECEIVER, "Caller Wait Receiver");
                break;
            default:
                break;
        }

        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        activeFragment.setArguments(getIntent().getExtras());

        getSupportActionBar().setDisplayHomeAsUpEnabled(!isReady);

        return activeFragment;
    }

    private void updateCallerState(int state, String requestId) {
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
    public void onRequestAccepted(int nextState, String requestId) {
        updateCallerState(nextState, requestId);
    }

    @Override
    public void onCallerCallReceiver(int celukState, String requestId) {
        updateCallerState(celukState, requestId);
    }

    @Override
    public void onReceiverStop(int celukState) {
        updateCallerState(celukState, null);
    }

    @Override
    public void onEndCELUKPairing(int celukState) {
        updateCallerState(celukState, null);
    }

    @Override
    public void onChangeCallerLocation(double latitude, double longitude) {
        CelukUser user = shared.getCurrentUser();
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        shared.setCurrentUser(user);

        mDatabase.child("users")
                .child(getUserUid())
                .setValue(user);
    }
}
