package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText editMessage;
    private DatabaseReference mDatabase;
    private RecyclerView mMessageList;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mCurrentUser;
    private static String currentUserName;
    private DatabaseReference mDatabaseUsers;

    private static final String TAG = "MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        editMessage = (EditText) findViewById(R.id.editMessageE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Messages");

        mMessageList = (RecyclerView) findViewById(R.id.messageRec);
        mMessageList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); // makes message list start displaying from the bottom of screen
        mMessageList.setLayoutManager(linearLayoutManager);

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

    public void sendButtonClicked(View view) {
        mCurrentUser = mAuth.getCurrentUser();
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        FirebaseApp.initializeApp(this);
        final String messageValue = editMessage.getText().toString().trim();
        if(!TextUtils.isEmpty(messageValue)){
            final DatabaseReference newPost = mDatabase.push();
            mDatabaseUsers.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    newPost.child("content").setValue(messageValue);
                    newPost.child("username").setValue(dataSnapshot.child("Name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            mMessageList.scrollToPosition(mMessageList.getAdapter().getItemCount()); // sets the recycler view to last message
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        /* this ValueEventListener reads and obtains the username of the currently logged in user from firebase,
         this initializes currentUserName, and is then used in MessageViewHolder to
         setBackgroundColor and setAlignment of each individual message2
         */
//        FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid())
//                .addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        // This method is called once with the initial value and again
//                        // whenever data at this location is updated.
//                        String name = dataSnapshot.child("Name").getValue().toString();
//                        Log.d(TAG, "Value is: " + name);
//                        currentUserName = name;
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError error) {
//                        // Failed to read value
//                        Log.w(TAG, "Failed to read value.", error.toException());
//                    }
//                });

        FirebaseRecyclerAdapter<Message, MessageViewHolder> FBRA = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(
                Message.class,
                R.layout.single_message_layout,
                MessageViewHolder.class,
                mDatabase
        ) {
            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, Message model, int position) {
                viewHolder.setContent(model.getContent());
                viewHolder.setUsername(model.getUsername());
                viewHolder.setBackgroundColor(model.getUsername());
                viewHolder.setAlignment(model.getUsername());

            }
        };
        mMessageList.setAdapter(FBRA);

    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder{
        // note this class is usually static but had to gain access to an
        View mView;


        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setBackgroundColor(String username) {
            // if the message username is not the same as the logged in username then it is displayed in blue
//            if(!currentUserName.equals(username)) {
//                LinearLayout message_layout = (LinearLayout) mView.findViewById(R.id.messageLinearLayout);
//                message_layout.setBackgroundResource(R.drawable.blue_message_shape);
//            }
        }

        public void setAlignment(String username){
            // if the message username is not the same as the logged in username then it is right aligned
//            if(!currentUserName.equals(username)) {
//                LinearLayout message_outer_layout = (LinearLayout) mView.findViewById(R.id.messageOuterLinearLayout);
//                message_outer_layout.setGravity(5);
//            }
        }

        public void setContent(String content) {
            TextView message_content = (TextView) mView.findViewById(R.id.messageText);
            message_content.setText(content);
        }
        public void setUsername(String username){
            TextView username_content = (TextView) mView.findViewById(R.id.usernameText);
            username_content.setText(username);
        }
    }

    public void signOutBtnClicked(View view){
        mAuth.signOut();
    }


}
