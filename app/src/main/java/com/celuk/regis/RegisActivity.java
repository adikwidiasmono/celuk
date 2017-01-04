package com.celuk.regis;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.celuk.database.model.CelukUser;
import com.celuk.main.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.utils.AppDateUtils;
import com.utils.AppUtils;
import com.utils.CelukSharedPref;
import com.utils.CelukState;

public class RegisActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilPhone;
    private EditText etEmail, etName, etPhone;
    private Button btCreateUser;

    private DatabaseReference mUserReference;

    private CelukSharedPref shared;

    private String email, userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regis);

        email = getIntent().getStringExtra("email");
        userUid = getIntent().getStringExtra("user_uid");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUserReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userUid);

        shared = new CelukSharedPref(getApplicationContext());

        initView();
        initAction();
    }

    private void initView() {
        tilName = (TextInputLayout) findViewById(R.id.til_name);
        tilPhone = (TextInputLayout) findViewById(R.id.til_phone);

        etEmail = (EditText) findViewById(R.id.et_email);
        etName = (EditText) findViewById(R.id.et_name);
        etPhone = (EditText) findViewById(R.id.et_phone);

        btCreateUser = (Button) findViewById(R.id.bt_create_user);

        etEmail.setText(email);
        etEmail.setEnabled(false);
    }

    private void initAction() {
        btCreateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tilName.setError(null);
                tilPhone.setError(null);

                if (TextUtils.isEmpty(etName.getText().toString())) {
                    tilName.setError(getString(R.string.error_field_required));
                    etName.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(etPhone.getText().toString())) {
                    tilPhone.setError(getString(R.string.error_field_required));
                    etPhone.requestFocus();
                    return;
                }

                createCelukUser(email, etName.getText().toString(), etPhone.getText().toString());
            }
        });
    }

    private void createCelukUser(String email, String name, String phone) {
        CelukUser celukUser = new CelukUser();
        celukUser.setEmail(email);
        celukUser.setName(name);
        celukUser.setPhone(phone);
        celukUser.setCreatedDate(AppDateUtils.getCurrentDate(AppDateUtils.APP_DATE_PATTERN));
        celukUser.setPairedState(CelukState.CELUK_NO_ASSIGNMENT);
        mUserReference.setValue(celukUser);

        shared.setCurrentUser(celukUser);

        AppUtils.routeCelukUser(RegisActivity.this, celukUser);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
