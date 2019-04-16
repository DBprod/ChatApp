package com.example.chatapp;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;

public class MainActivity extends AppCompatActivity{

    private DatabaseReference currentUserRef;
    private DatabaseReference receiverUserDB;
    private RecyclerView mContactList;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseRecyclerAdapter<People, PeopleHolder> adapter;
    private Menu menu;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;
    private BigInteger[] myPublicKey = new BigInteger[2];
    private boolean menuCreated = false;
    private boolean correctKeyInput = false;

    private static final String TAG = "MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        preferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        myPublicKey[0] = new BigInteger(preferences.getString("mod", "1"));
        myPublicKey[1] = new BigInteger(preferences.getString("exp", "1"));

        mContactList = (RecyclerView) findViewById(R.id.contactRec);
        mContactList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
       // linearLayoutManager.setStackFromEnd(true); // makes message list start displaying from the bottom of screen
        mContactList.setLayoutManager(linearLayoutManager);


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

        currentUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid()).child("Contacts");

        getSupportActionBar().setTitle("Your Messages");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        Query query = currentUserRef;
        FirebaseRecyclerOptions<People> options = new FirebaseRecyclerOptions.Builder<People>().setQuery(query, People.class).build();
        adapter = new FirebaseRecyclerAdapter<People, PeopleHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final PeopleHolder holder, int position, @NonNull People model) {
                final String uid = model.getContactId();
                DatabaseReference receiverUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
                holder.setContent(Encryptor.decrypt(model.getContent(), myPublicKey, new BigInteger(Encryptor.privateKey)));
                receiverUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String receiverName = dataSnapshot.child("name").getValue().toString();
                        final String receiverMod = dataSnapshot.child("mod").getValue().toString();
                        final String receiverExp = dataSnapshot.child("exp").getValue().toString();

                        holder.setUsername(receiverName);
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

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

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

        if(menuCreated){
            correctKeyInput = Encryptor.checkKeys(myPublicKey);
            setMenuKeyText(correctKeyInput);
        }
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
            TextView nameView = mView.findViewById(R.id.nameField);
            nameView.setText(username);
        }

        public void setContent(String content){
            TextView contentView = mView.findViewById(R.id.recentMessage);
            contentView.setText(content);
        }
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        this.menu = menu;
        setMenuKeyText(Encryptor.checkKeys(myPublicKey));
        menuCreated = true;
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logoutBtn) {
            prefEditor.clear().commit();
            mAuth.signOut();
        }
        if (id == R.id.privateKeyInput){
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if(!correctKeyInput) {
                ClipData clip = clipboard.getPrimaryClip();
                String privateKey = clip.getItemAt(0).coerceToText(this).toString();
                try {
                    new BigInteger(privateKey);
                    Encryptor.privateKey = privateKey;
                    if (Encryptor.checkKeys(myPublicKey)) {
                        menu.findItem(R.id.privateKeyInput).setTitle("Remove Private Key");
                        correctKeyInput = true;
                        adapter.notifyDataSetChanged();
                    } else {
                        menu.findItem(R.id.privateKeyInput).setTitle("Set Private Key");
                        Toast.makeText(this, "Incorrect Private Key", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Encryptor.privateKey = "1";
                }
            }
            else{
                Encryptor.privateKey = "1";
                correctKeyInput = false;
                setMenuKeyText(false);
                adapter.notifyDataSetChanged();
            }
            ClipData emptyClip = ClipData.newPlainText("", "");
            clipboard.setPrimaryClip(emptyClip);
        }
        return super.onOptionsItemSelected(item);
    }

    public void setMenuKeyText(boolean validKey){
        MenuItem menuItem = menu.findItem(R.id.privateKeyInput);
        if(validKey){
            menuItem.setTitle("Remove Private Key");
        } else{
            menuItem.setTitle("Set Private Key");
        }
    }
}
