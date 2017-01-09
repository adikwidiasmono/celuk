package com.celuk.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.celuk.database.model.CelukUser;
import com.celuk.regis.RegisActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.utils.AppUtils;
import com.utils.CelukSharedPref;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getCanonicalName();
    // Firebase DB
    protected DatabaseReference mDatabase;
    // UI references.
    private EditText etEmail, etPassword;
    private View mProgressView, mLoginFormView;
    private TextInputLayout tilEmail, tilPassword;
    private TextView tvAuthStatus;
    // Firebase auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    // Firebase User DB
    private Query mUserQuery;
    private ValueEventListener mUserListener;
    private CelukSharedPref shared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        shared = new CelukSharedPref(getApplicationContext());

        // Check have user been loged in
        if (shared.getCurrentUser() != null) {
            AppUtils.routeCelukUser(LoginActivity.this, shared.getCurrentUser());
            return;
        }

        // Set up the login form.
        tilEmail = (TextInputLayout) findViewById(R.id.til_email);
        etEmail = (EditText) findViewById(R.id.et_email);

        tilPassword = (TextInputLayout) findViewById(R.id.til_password);
        etPassword = (EditText) findViewById(R.id.et_password);
        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    signIn();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        tvAuthStatus = (TextView) findViewById(R.id.tv_auth_status);
        tvAuthStatus.setVisibility(View.GONE);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.pb_login_progress);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onAuthSuccess(user);
                } else {
                    shared.setCurrentUser(null);
                    // User is signed out
                    Log.e(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuthListener != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        if (mUserListener != null) {
            mUserQuery.removeEventListener(mUserListener);
        }
    }

    private void onAuthSuccess(FirebaseUser user) {
        Log.e(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
        Log.e(TAG, "|" + user.getDisplayName() + "|");
        Log.e(TAG, "|" + user.getEmail() + "|");
        Log.e(TAG, "|" + user.getProviderId() + "|");

        initActiveUser(user.getUid(), user.getEmail());
    }

    private void initActiveUser(final String userUid, final String email) {
        mUserQuery = mDatabase.child("users").child(userUid);
        mUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Success login here
                showProgress(false);

                if (snapshot.exists()) {
                    shared.setCurrentUser(snapshot.getValue(CelukUser.class));
                    AppUtils.routeCelukUser(LoginActivity.this, shared.getCurrentUser());
                    finish();
                } else {
                    // Redirect to create new user
                    Intent intent = getIntent();
                    intent.setClass(getApplicationContext(), RegisActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("user_uid", userUid);
                    intent.putExtra("email", email);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mUserQuery.addValueEventListener(mUserListener);
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void signIn() {
        // Hide soft keyboard
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        // Store values at the time of the login attempt.
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        Log.e(TAG, "signIn:" + email);
        if (!validateForm(email, password)) {
            return;
        }

        Log.e(TAG, "Preparing login");
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.e(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();

                            tvAuthStatus.setText(R.string.auth_failed);
                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private boolean validateForm(String email, String password) {
        // Reset errors.
        tilEmail.setError(null);
        tilPassword.setError(null);

        boolean isValid = true;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_field_required));
            focusView = etPassword;
            isValid = false;
        } else if (!isPasswordValid(password)) {
            tilPassword.setError(getString(R.string.error_invalid_password));
            focusView = etPassword;
            isValid = false;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_field_required));
            focusView = etEmail;
            isValid = false;
        } else if (!AppUtils.isEmailValid(email)) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            focusView = etEmail;
            isValid = false;
        }

        if (!isValid) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
        }

        return isValid;
    }

}

