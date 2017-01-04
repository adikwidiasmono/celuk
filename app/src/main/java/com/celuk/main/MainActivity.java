package com.celuk.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.celuk.caller.CallerActivity;
import com.celuk.parent.BaseActivity;
import com.celuk.receiver.ReceiverActivity;
import com.utils.CelukSharedPref;

public class MainActivity extends BaseActivity {

    private TextView tvSignOut, tvUserEmail, tvCaller, tvReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvSignOut = (TextView) findViewById(R.id.tv_sign_out);
        tvUserEmail = (TextView) findViewById(R.id.tv_user_email);
        tvCaller = (TextView) findViewById(R.id.tv_caller);
        tvReceiver = (TextView) findViewById(R.id.tv_receiver);

        tvUserEmail.setText(new CelukSharedPref(getApplicationContext())
                .getCurrentUser().getEmail());

        tvSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        tvCaller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                intent.setClass(getApplicationContext(), CallerActivity.class);
                intent.putExtra("READY", false);
                startActivity(intent);
            }
        });

        tvReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                intent.setClass(getApplicationContext(), ReceiverActivity.class);
                intent.putExtra("READY", false);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
