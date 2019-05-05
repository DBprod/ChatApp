package com.example.chatapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
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

import java.math.BigInteger;

public class MessageActivity extends AppCompatActivity implements LogoutDialog.LogoutDialogListener {
    private String receiver_name = null;
    private String receiver_uid = null;
    private DatabaseReference senderMessageRef;
    private DatabaseReference mDatabase;
    private RecyclerView mMessageList;
    private FirebaseUser mUser;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mReceiverRef;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private BigInteger[] myPublicKey = new BigInteger[2];
    private BigInteger[] receiverPublicKey =  new BigInteger[2];
    private FirebaseRecyclerAdapter<Message, MessageHolder> adapter;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;
    private Menu menu;
    private DatabaseReference UsersDatabase;

    private EditText editMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        FirebaseApp.initializeApp(this);

        preferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        editMessage = (EditText) findViewById(R.id.editMessage);

        receiver_name = getIntent().getExtras().getString("receiverName");
        receiver_uid = getIntent().getExtras().getString("uid");

        mReceiverRef = FirebaseDatabase.getInstance().getReference().child("Messages").child(receiver_uid).child("Messages");

        mMessageList = (RecyclerView) findViewById(R.id.messageRec);
        mMessageList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true); // makes message list start displaying from the bottom of screen
        mMessageList.setLayoutManager(linearLayoutManager);

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        senderMessageRef = FirebaseDatabase.getInstance().getReference().child("Messages").child(mUser.getUid());
        mDatabase = senderMessageRef.child("Messages");
        UsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        myPublicKey[0] = new BigInteger(preferences.getString("mod", "1"));
        myPublicKey[1] = new BigInteger(preferences.getString("exp", "1"));
        if(myPublicKey[0].equals("0") || myPublicKey[1].equals("0"))
            System.out.print("Failed to get Public Key. MessageActivity.java line 87");

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
                String plainText = Encryptor.decrypt(cipherInt, myPublicKey, new BigInteger(Encryptor.privateKey), emoji);
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
        FirebaseApp.initializeApp(this);
        final String messageValue = editMessage.getText().toString().trim();
        if(!TextUtils.isEmpty(messageValue)){
            boolean emoji = false;
            final BigInteger messageInt = new BigInteger(messageValue.getBytes());
            if(messageInt.compareTo(new BigInteger("0")) == -1)
                emoji = true;

            final String senderEncryptedMessage = Encryptor.encrypt(messageInt, myPublicKey).toString();
            final String receiverEncryptedMessage = Encryptor.encrypt(messageInt, receiverPublicKey).toString();

            //send message to yourself
            final long timestamp = System.currentTimeMillis();
            final DatabaseReference senderPost = mDatabase.child(receiver_uid).push();
            final DatabaseReference senderRecentPost = UsersDatabase.child(mCurrentUser.getUid()).child("Contacts");

            //Sends message to Contacts
            DatabaseReference recentRef = senderRecentPost.child(receiver_uid);
            recentRef.child("content").setValue(senderEncryptedMessage);
            recentRef.child("contactId").setValue(receiver_uid);
            recentRef.child("timestamp").setValue(timestamp);
            recentRef.child("sender").setValue(1);

            //Sends message to Messages
            senderPost.child("content").setValue(senderEncryptedMessage);
            senderPost.child("sender").setValue(1);
            senderPost.child("emoji").setValue(emoji);

            //send message to other


            if(!receiver_uid.equals(mCurrentUser.getUid())) {
                final DatabaseReference receiverPost = mReceiverRef.child(mUser.getUid()).push();
                final DatabaseReference receiverRecentPost = UsersDatabase.child(receiver_uid).child("Contacts");

                //Sends message to Contacts
                recentRef = receiverRecentPost.child(mCurrentUser.getUid());
                recentRef.child("content").setValue(receiverEncryptedMessage);
                recentRef.child("contactId").setValue(mCurrentUser.getUid());
                recentRef.child("timestamp").setValue(timestamp);
                recentRef.child("sender").setValue(0);

                //Sends message to Messages
                receiverPost.child("content").setValue(receiverEncryptedMessage);
                receiverPost.child("sender").setValue(0);
                receiverPost.child("emoji").setValue(emoji);
            }
        }

        editMessage.setText("");
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_menu, menu);
        this.menu = menu;
        setMenuKeyIcon(Encryptor.checkKeys(myPublicKey));
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logoutBtn) {
            openLogoutDialog();
        }

        if(id == R.id.settingBtn){
            Intent settingIntent = new Intent(MessageActivity.this, SettingActivity.class);
            settingIntent.putExtra("uid",mAuth.getCurrentUser().getUid());
            startActivity(settingIntent);
        }

        if (id == R.id.uidBtn){
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("uid", mAuth.getCurrentUser().getUid());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "ID copied to Clipboard", Toast.LENGTH_SHORT).show();
        }

        if (id == R.id.privateKeyInput) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (!Encryptor.correctKey) {
                ClipData clip = clipboard.getPrimaryClip();
                try {
                    String clipboardString = clip.getItemAt(0).coerceToText(this).toString().trim();
                    try {
                        new BigInteger(clipboardString);
                        Encryptor.privateKey = clipboardString;
                        if (Encryptor.checkKeys(myPublicKey)) {
                            setMenuKeyIcon(true);
                            ClipData emptyClip = ClipData.newPlainText("", "");
                            clipboard.setPrimaryClip(emptyClip);
                            Encryptor.correctKey = true;
                            Toast.makeText(this, "Messages Successful Encrypted", Toast.LENGTH_SHORT).show();
                            lockSound();
                            adapter.notifyDataSetChanged();
                        } else {
                            vibrate();
                            Toast.makeText(this, "Clipboard private key is incorrect", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        vibrate();
                        if (clipboardString.isEmpty())
                            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(this, "Clipboard private key is incorrect", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e){
                    vibrate();
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                }
            } else{
                Encryptor.privateKey = "1";
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Messages are now Encrypted", Toast.LENGTH_SHORT).show();
                lockSound();
                Encryptor.correctKey = false;
                Encryptor.privateKey = "1";
            }
            setMenuKeyIcon(Encryptor.correctKey);
        }
        return super.onOptionsItemSelected(item);
    }

    public void setMenuKeyIcon(boolean validKey){
        MenuItem menuItem = menu.findItem(R.id.privateKeyInput);
        if(validKey){
            menuItem.setIcon(R.drawable.ic_lock_open);
        } else{
            menuItem.setIcon(R.drawable.ic_lock_closed);
        }
    }

    public void openLogoutDialog() {
        LogoutDialog logoutDialog = new LogoutDialog();
        logoutDialog.show(getSupportFragmentManager(), "logout dialog");
    }

    @Override
    public void logout(boolean logout) {
        if(logout){
            mAuth.signOut();
            prefEditor.clear().commit();
            Encryptor.privateKey = "1";
        }
    }

    public void vibrate(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(60);
        }
    }

    public void lockSound(){
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.lock_sound);
        mediaPlayer.start();
    }
}
