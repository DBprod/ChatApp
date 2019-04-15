package com.example.chatapp;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.math.BigInteger;

public class MainActivity extends AppCompatActivity{

    private DatabaseReference mDatabase;
    private RecyclerView mContactList;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseRecyclerAdapter<People, PeopleHolder> adapter;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    private static final String TAG = "MESSAGE";

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        preferences = getSharedPreferences("privateKeyPreference", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        mContactList = (RecyclerView) findViewById(R.id.contactRec);
        mContactList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
       // linearLayoutManager.setStackFromEnd(true); // makes message list start displaying from the bottom of screen
        mContactList.setLayoutManager(linearLayoutManager);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() ==  null){
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // prevents user from going back to previous activity
                    startActivity(loginIntent);
                    finish();
                }
            }
        };

        getSupportActionBar().setTitle("Your Messages");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        Query query = mDatabase;
        FirebaseRecyclerOptions<People> options = new FirebaseRecyclerOptions.Builder<People>().setQuery(query, People.class).build();
        adapter = new FirebaseRecyclerAdapter<People, PeopleHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull PeopleHolder holder, int position, @NonNull People model) {
                holder.setUsername(model.getName());
                final String uid = model.getUid();
                final String receiverExp = model.getExp();
                final String receiverMod = model.getMod();

                final String receiverName = model.getName();
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent viewMessagesIntent = new Intent(MainActivity.this, MessageActivity.class);
                        viewMessagesIntent.putExtra("uid", uid);
                        viewMessagesIntent.putExtra("receiverName", receiverName);
                        viewMessagesIntent.putExtra("receiverMod", receiverMod);
                        viewMessagesIntent.putExtra("receiverExp", receiverExp);
                        startActivity(viewMessagesIntent);
                    }
                });
            }

            @NonNull
            @Override
            public PeopleHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.single_contact, viewGroup, false);
                return new PeopleHolder(view);
            }
        };
        mContactList.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public static class PeopleHolder extends RecyclerView.ViewHolder{
        // note this class is usually static but had to gain access to an
        View mView;


        public PeopleHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }


        public void setUsername(String username){
            TextView name_content = (TextView) mView.findViewById(R.id.nameField);
            name_content.setText(username);
        }
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logoutBtn) {
            mAuth.signOut();
            prefEditor.clear().commit();
        }
        if (id == R.id.privateKeyInput){
            try{
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = clipboard.getPrimaryClip();
                String privateKey = clip.getItemAt(0).coerceToText(this).toString();
                new BigInteger(privateKey);
                prefEditor.putString("privateKey", privateKey);
            }
            catch(Exception e){
                prefEditor.putString("privateKey", "1");
            } finally {
                prefEditor.commit();
            }
        }
//        if (id == R.id.privateKeyRemove){
//            prefEditor.putString("privateKey", "1");
//        }
        return super.onOptionsItemSelected(item);
    }
}
