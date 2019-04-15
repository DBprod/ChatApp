package com.example.chatapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;

public class MessageActivity extends AppCompatActivity{
    private String receiver_name = null;
    private String receiver_uid = null;
    private DatabaseReference senderDatabase;
    private DatabaseReference mDatabase;
    private RecyclerView mMessageList;
    private FirebaseUser mUser;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mDatabaseUsers;
    private DatabaseReference mReceiverRef;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private BigInteger[] myPublicKey = new BigInteger[2];
    private BigInteger[] receiverPublicKey =  new BigInteger[2];
    FirebaseRecyclerAdapter<Message, MessageHolder> adapter;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    private EditText editMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        FirebaseApp.initializeApp(this);

        preferences = getSharedPreferences("privateKeyPreference", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        editMessage = (EditText) findViewById(R.id.editMessage);

        receiver_name = getIntent().getExtras().getString("receiverName");
        receiver_uid = getIntent().getExtras().getString("uid");

        mReceiverRef = FirebaseDatabase.getInstance().getReference().child("Users").child(receiver_uid).child("Messages");

        mMessageList = (RecyclerView) findViewById(R.id.messageRec);
        mMessageList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); // makes message list start displaying from the bottom of screen
        mMessageList.setLayoutManager(linearLayoutManager);

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        senderDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mUser.getUid());
        mDatabase = senderDatabase.child("Messages");

        senderDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myPublicKey[0] = new BigInteger(dataSnapshot.child("mod").getValue().toString());
                myPublicKey[1] = new BigInteger(dataSnapshot.child("exp").getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        receiverPublicKey[0] = new BigInteger(getIntent().getExtras().getString("receiverMod"));
        receiverPublicKey[1] = new BigInteger(getIntent().getExtras().getString("receiverExp"));

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() ==  null){
                    Intent loginIntent = new Intent(MessageActivity.this,RegisterActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // prevents user from going back to previous activity
                    startActivity(loginIntent);
                }
            }
        };

        getSupportActionBar().setTitle(receiver_name+"");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Query query = mDatabase.child(receiver_uid);
        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>().setQuery(query, Message.class).build();
        adapter = new FirebaseRecyclerAdapter<Message, MessageHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MessageHolder holder, int position, @NonNull Message model) {
                String cipherInt = model.getContent();
                boolean emoji = model.isEmoji();
                String plainText = Encryptor.decrypt(cipherInt, myPublicKey, new BigInteger(preferences.getString("privateKey", "1")), emoji);
                holder.setContent(plainText);
            }

            @NonNull
            @Override
            public MessageHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                View view = null;
                switch (viewType){
                    case 0: {
                        view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.their_chat_bubble, viewGroup, false);
                        break;
                    }
                    case 1: {
                        view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.my_chat_bubble, viewGroup, false);
                        break;
                    }
                    default: {
                        view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.my_chat_bubble, viewGroup, false);
                        break;
                    }
                }
                return new MessageHolder(view);
            }

            @Override
            public int getItemViewType(int position) {
                return getItem(position).getSender();
            }
        };

        mMessageList.setAdapter(adapter);
        adapter.startListening();

        mDatabase.child(receiver_uid).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                mMessageList.smoothScrollToPosition(mMessageList.getAdapter().getItemCount());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public static class MessageHolder extends RecyclerView.ViewHolder{
        // note this class is usually static but had to gain access to an
        View mView;


        public MessageHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setContent(String content){
            TextView message_content = (TextView) mView.findViewById(R.id.messageText);
            message_content.setText(content);
        }
    }

    public void sendButtonClicked(View view) {
        mCurrentUser = mAuth.getCurrentUser();
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
        FirebaseApp.initializeApp(this);
        final String messageValue = editMessage.getText().toString().trim();
        if(!TextUtils.isEmpty(messageValue)){
            boolean emoji = false;
            final BigInteger messageInt = new BigInteger(messageValue.getBytes());
            if(messageInt.compareTo(new BigInteger("0")) == -1)
                emoji = true;

            final String myEncryptedMessage = Encryptor.encrypt(messageInt, myPublicKey).toString();
            final String receiverEncryptedMessage = Encryptor.encrypt(messageInt, receiverPublicKey).toString();


            //send message to yourself
            final DatabaseReference senderPost = mDatabase.child(receiver_uid).push();
            senderPost.child("content").setValue(myEncryptedMessage);
//            senderPost.child("chatId").setValue(receiver_uid);
            senderPost.child("sender").setValue(1);
            senderPost.child("emoji").setValue(emoji);
            //send message to other

            if(!receiver_uid.equals(mCurrentUser.getUid())) {
                final DatabaseReference receiverPost = mReceiverRef.child(mUser.getUid()).push();
                receiverPost.child("content").setValue(receiverEncryptedMessage);
//                receiverPost.child("chatId").setValue(mUser.getUid());
                receiverPost.child("sender").setValue(0);
                receiverPost.child("emoji").setValue(emoji);
            }
        }

        editMessage.setText("");
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
            prefEditor.putString("privateKey", null).commit();
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
                adapter.notifyDataSetChanged();
            }
        }
        if (id == R.id.privateKeyRemove){
            prefEditor.putString("privateKey", "1").commit();
            adapter.notifyDataSetChanged();
        }
        return super.onOptionsItemSelected(item);
    }
}
