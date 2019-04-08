package com.example.chatapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class PrivateKeyActivity extends AppCompatActivity {
    private String privateKey;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_key);

        continueButton = findViewById(R.id.continueBtn);
    }

    public void copyTextBtnClicked(View view){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(this.CLIPBOARD_SERVICE);
        privateKey = getIntent().getExtras().getString("privateKey");
        ClipData clip = ClipData.newPlainText("privateKey", privateKey);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Your private key has been copied", Toast.LENGTH_LONG).show();
        continueButton.setVisibility(View.VISIBLE);
    }

    public void continueBtnClicked(View view){
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }
}
