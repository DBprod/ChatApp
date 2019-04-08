package com.example.chatapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class PrivateKeyActivity extends AppCompatActivity {

    private TextView privateKeyText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_key);

        privateKeyText = (TextView) findViewById(R.id.privateKeyTextView);




    }

    public void copyTextBtnClicked(View view){

    }

    public void continueBtnClicked(View view){

    }
}
