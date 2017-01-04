package com.celuk.caller;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;

import com.celuk.database.model.CelukUser;
import com.celuk.main.R;
import com.celuk.parent.BaseActivity;
import com.utils.CelukState;

public class CallerActivity extends BaseActivity implements
        CallerReadyFragment.OnFragmentInteractionListener {

    private boolean isReady;
    private boolean isResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isReady = getIntent().getBooleanExtra("READY", false);

        setupFragment(savedInstanceState);
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
        if (isReady)
            moveTaskToBack(true);
        else
            super.onBackPressed();
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
                activeFragment = CallerTrackerFragment.newInstance(CelukState.CALLER_CALL_RECEIVER, "Caller Call Receiver");
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

        return activeFragment;
    }

    private void updateCallerState(int state) {
        CelukUser user = shared.getCurrentUser();
        user.setPairedState(state);
        shared.setCurrentUser(user);

        mDatabase.child("users")
                .child(getUserUid())
                .child("pairedState")
                .setValue(state);

        updateActiveFragment(getActiveFragment());
    }

    @Override
    public void onCallerCallReceiver(int nextState) {
        updateCallerState(nextState);
    }

    @Override
    public void onReceiverStop(int nextState) {
        updateCallerState(nextState);
    }
}
