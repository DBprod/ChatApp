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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.math.BigInteger;

public class RegisterActivity extends AppCompatActivity {

    private EditText name,email,password, confirmPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        name = (EditText) findViewById(R.id.editUsername);
        email = (EditText) findViewById(R.id.editEmail);
        password = (EditText) findViewById(R.id.editPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        preferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();
    }

    public void signupButtonClicked(View view){

        final String name_content,password_content,email_content,confirmPassword_content;

        name_content= name.getText().toString().trim();
        password_content = password.getText().toString().trim();
        email_content=  email.getText().toString().trim();
        confirmPassword_content = confirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email_content)
                || TextUtils.isEmpty(name_content)
                || TextUtils.isEmpty(password_content)){
            Toast.makeText(this, "Please fill the sign up form", Toast.LENGTH_LONG).show();
        }
        else if(!confirmPassword_content.equals(password_content)){
            Toast.makeText(this, "Your passwords do not match", Toast.LENGTH_LONG).show();
        }
        else{
            mAuth.createUserWithEmailAndPassword(email_content,password_content).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        String user_id = mAuth.getCurrentUser().getUid();
                        DatabaseReference current_user_db = mDatabase.child(user_id);
                        current_user_db.child("name").setValue(name_content);
                        current_user_db.child("uid").setValue(mAuth.getUid());

                        BigInteger[] primes = Encryptor.generatePrimes();
                        BigInteger[] publicKey = Encryptor.generatePublicKey(primes[0], primes[1]);

                        // Public key logic and storing into database
                        String mod = publicKey[0].toString();
                        String exp = publicKey[1].toString();
                        current_user_db.child("mod").setValue(mod);
                        current_user_db.child("exp").setValue(exp);
                        prefEditor.putString("mod", mod).commit();
                        prefEditor.putString("exp", exp).commit();

                        //Generating private key to send to the private key activity
                        String privateKey = Encryptor.generatePrivateKey(publicKey, primes[0], primes[1]).toString();

                        Intent privateKeyIntent = new Intent(RegisterActivity.this, PrivateKeyActivity.class);
                        privateKeyIntent.putExtra("privateKey", privateKey);
                        startActivity(privateKeyIntent);
                    }
                }
            });
        }
    }
    public void loginButtonClicked(View view){
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    }
}
