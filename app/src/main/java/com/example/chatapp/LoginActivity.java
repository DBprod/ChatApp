package com.example.chatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail;
    private EditText loginPass;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = (EditText) findViewById(R.id.loginEmail);
        loginPass = (EditText) findViewById(R.id.loginPass);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        preferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        getSupportActionBar().setTitle("Login");
    }
    public void loginButtonClicked(View view) {
        String email = loginEmail.getText().toString().trim();
        String pass = loginPass.getText().toString().trim();
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass)) {
            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        checkUserExists();
                    }
                    else {
                        Toast.makeText(LoginActivity.this, "The username password is incorrect", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Toast.makeText(LoginActivity.this, "Input an email and a password", Toast.LENGTH_LONG).show();
        }
    }

    public void checkUserExists() {
        final String user_id = mAuth.getCurrentUser().getUid();
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(user_id)) {
                    mDatabase.child(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            prefEditor.putString("mod", dataSnapshot.child("mod").getValue().toString()).commit();
                            prefEditor.putString("exp", dataSnapshot.child("exp").getValue().toString()).commit();
                            Intent loginIntent = new Intent(LoginActivity.this, MainActivity.class);
                            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // prevents user from going back to previous activity
                            startActivity(loginIntent);
                            finish();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void forgotPasswordBtnClicked(View view){
        try {
            final String userEmail = loginEmail.getText().toString().trim();
            mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password Reset Email Sent To: "+ userEmail, Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(LoginActivity.this, "There does not seem to be an account under this email", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Please input a valid email", Toast.LENGTH_LONG).show();
        }
    }

    public void createAccountBtnClicked(View view){
        Intent loginIntent = new Intent(this, RegisterActivity.class);
        startActivity(loginIntent);
    }
}
