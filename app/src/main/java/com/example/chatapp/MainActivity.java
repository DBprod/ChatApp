package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private RecyclerView mContactList;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private static final String TAG = "MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        mContactList = (RecyclerView) findViewById(R.id.contactRec);
        mContactList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); // makes message list start displaying from the bottom of screen
        mContactList.setLayoutManager(linearLayoutManager);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() ==  null){
                    Intent loginIntent = new Intent(MainActivity.this,RegisterActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // prevents user from going back to previous activity
                    startActivity(loginIntent);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        Query query = mDatabase;
        FirebaseRecyclerOptions<People> options = new FirebaseRecyclerOptions.Builder<People>().setQuery(query, People.class).build();
        FirebaseRecyclerAdapter<People, PeopleHolder> adapter = new FirebaseRecyclerAdapter<People, PeopleHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull PeopleHolder holder, int position, @NonNull People model) {
                holder.setUsername(model.getName());
                final String name = model.getName();
                final String uid = model.getUid();
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent viewMessagesIntent = new Intent(MainActivity.this, MessageActivity.class);
                        viewMessagesIntent.putExtra("receiveName", name);
                        viewMessagesIntent.putExtra("uid", uid);
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

    public void signOutBtnClicked(View view){
        mAuth.signOut();
    }


}
