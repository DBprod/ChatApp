package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

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

public class MessageActivity extends AppCompatActivity {

    private String receiver_name = null;
    private String receiver_uid = null;
    private DatabaseReference mDatabase;
    private RecyclerView mMessageList;
    private FirebaseUser mUser;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mDatabaseUsers;
    private DatabaseReference mReceiverRef;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private EditText editMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        FirebaseApp.initializeApp(this);

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

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mUser.getUid()).child("Messages");


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

    }

    public void sendButtonClicked(View view) {
        mCurrentUser = mAuth.getCurrentUser();
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        FirebaseApp.initializeApp(this);
        final String messageValue = editMessage.getText().toString().trim();
        if(!TextUtils.isEmpty(messageValue)){
            final DatabaseReference senderPost = mDatabase.push();
            senderPost.child("content").setValue(messageValue);
            senderPost.child("receiver").setValue(receiver_name);
            senderPost.child("chatId").setValue(receiver_uid);
            senderPost.child("sender").setValue(1);

            final DatabaseReference receiverPost = mReceiverRef.push();
            receiverPost.child("content").setValue(messageValue);
            receiverPost.child("receiver").setValue(receiver_name);
            receiverPost.child("chatId").setValue(mUser.getUid());
            receiverPost.child("sender").setValue(0);
        }

        mReceiverRef.addChildEventListener(new ChildEventListener() {
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
    protected void onStart() {
        super.onStart();

        Query query = mDatabase.orderByChild("chatId").equalTo(receiver_uid);
        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>().setQuery(query, Message.class).build();
        FirebaseRecyclerAdapter<Message, MessageHolder> adapter = new FirebaseRecyclerAdapter<Message, MessageHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MessageHolder holder, int position, @NonNull Message model) {
                holder.setContent(model.getContent());
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
                MessageHolder viewHolder = new MessageHolder(view);
                return viewHolder;
            }

            @Override
            public int getItemViewType(int position) {
                return getItem(position).getSender();
            }
        };

        mMessageList.setAdapter(adapter);
        adapter.startListening();
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

    public void signOutBtnClicked(View view){
        mAuth.signOut();
    }
}
