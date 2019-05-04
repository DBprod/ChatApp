package com.example.chatapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

public class MainActivity extends AppCompatActivity implements LogoutDialog.LogoutDialogListener {

    private DatabaseReference currentUserRef;
    private RecyclerView mContactList;
    public FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseRecyclerAdapter<People, PeopleHolder> adapter;
    private Menu menu;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;
    private BigInteger[] myPublicKey = new BigInteger[2];
    private boolean menuCreated = false;
    android.support.v7.widget.SearchView searchView;

    private static final String TAG = "MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (mAuth.getCurrentUser() ==  null){
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // prevents user from going back to previous activity
                    startActivity(loginIntent);
                    finish();
                }
            }
        };
        mAuth.addAuthStateListener(mAuthListener);

        FirebaseApp.initializeApp(this);

        preferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        myPublicKey[0] = new BigInteger(preferences.getString("mod", "1"));
        myPublicKey[1] = new BigInteger(preferences.getString("exp", "1"));

        if(mAuth.getCurrentUser() != null) {
            setContentView(R.layout.activity_main);
            mContactList = (RecyclerView) findViewById(R.id.contactRec);
            mContactList.setHasFixedSize(true);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setReverseLayout(true);
            linearLayoutManager.setStackFromEnd(true);
            mContactList.setLayoutManager(linearLayoutManager);
            getSupportActionBar().setTitle("Your Messages");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser() != null) {
            Query query = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid()).child("Contacts").orderByChild("timestamp");
            FirebaseRecyclerOptions<People> options = new FirebaseRecyclerOptions.Builder<People>().setQuery(query, People.class).build();
            adapter = new FirebaseRecyclerAdapter<People, PeopleHolder>(options) {
                @Override
                protected void onBindViewHolder(@NonNull final PeopleHolder holder, int position, @NonNull People model) {
                    final String uid = model.getContactId();
                    String recentMessage = Encryptor.decrypt(model.getContent(), myPublicKey, new BigInteger(Encryptor.privateKey));
                    if(model.getSender() == 1){
                        recentMessage = "You: " + recentMessage;
                    }
                    holder.setContent(recentMessage);
                    DatabaseReference receiverUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
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

                    final ImageView profilePic = view.findViewById(R.id.imageView3);
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef1 = database.getReference("imageURL/potat");;
                    final File[] localFile = new File[1];
                    final StorageReference storageref = FirebaseStorage.getInstance().getReference();
                    myRef1.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            try {
                                localFile[0] = File.createTempFile("images", "jpg");
                            } catch (IOException e) {
                                e.printStackTrace();
                                localFile[0] = null;
                                return;
                            }
                            storageref.child("images/fdsa.jpg").getFile(localFile[0])
                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            // Successfully downloaded data to local file
                                            // ...
                                            Picasso.with(MainActivity.this).load(localFile[0]).resize(70,70).centerCrop().into(profilePic, new Callback() {
                                                @Override
                                                public void onSuccess() {
                                                    Bitmap imgbmap = ((BitmapDrawable) profilePic.getDrawable()).getBitmap();
                                                    RoundedBitmapDrawable imgDrawable = RoundedBitmapDrawableFactory.create(getResources(), imgbmap);
                                                    imgDrawable.setCircular(true);
                                                    imgDrawable.setCornerRadius(35);
                                                    profilePic.setImageDrawable(imgDrawable);
                                                }
                                                @Override
                                                public void onError() {
                                                }
                                            });

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception exception) {
                                    // Handle failed download
                                    // ...
                                }
                            });
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                    if(localFile[0]==null) {
                        Picasso.with(MainActivity.this).load(R.drawable.avatar_icon).resize(70, 70).centerCrop().into(profilePic, new Callback() {
                            @Override
                            public void onSuccess() {
                                Bitmap imgbmap = ((BitmapDrawable) profilePic.getDrawable()).getBitmap();
                                RoundedBitmapDrawable imgDrawable = RoundedBitmapDrawableFactory.create(getResources(), imgbmap);
                                imgDrawable.setCircular(true);
                                imgDrawable.setCornerRadius(35);
                                profilePic.setImageDrawable(imgDrawable);
                            }

                            @Override
                            public void onError() {
                            }
                        });
                    }
                    return new PeopleHolder(view);
                }
            };
            mContactList.setAdapter(adapter);
            adapter.startListening();

            if(menuCreated)
                setMenuKeyIcon(Encryptor.correctKey);

            android.support.v7.widget.SearchView searchView = findViewById(R.id.searchButton);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    final String receiverId = s.trim();
                    DatabaseReference receiverUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(s);
                    receiverUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild("name")) {
                                final String receiverName = dataSnapshot.child("name").getValue().toString();
                                final String receiverMod = dataSnapshot.child("mod").getValue().toString();
                                final String receiverExp = dataSnapshot.child("exp").getValue().toString();
                                Intent viewMessagesIntent = new Intent(MainActivity.this, MessageActivity.class);
                                viewMessagesIntent.putExtra("uid", receiverId);
                                viewMessagesIntent.putExtra("receiverName", receiverName);
                                viewMessagesIntent.putExtra("receiverMod", receiverMod);
                                viewMessagesIntent.putExtra("receiverExp", receiverExp);
                                startActivity(viewMessagesIntent);
                            } else {
                                Toast.makeText(MainActivity.this, "This user does not exist", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(adapter != null){
            adapter.stopListening();
        }
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
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        setMenuKeyIcon(Encryptor.checkKeys(myPublicKey));
        menuCreated = true;
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.settingBtn){
            Intent settingIntent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(settingIntent);
        }

        if (id == R.id.logoutBtn) {
            openLogoutDialog();
        }

        if (id == R.id.uidBtn){
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("uid", mAuth.getCurrentUser().getUid());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "ID copied to Clipboard", Toast.LENGTH_SHORT).show();
        }

        if (id == R.id.privateKeyInput){
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if(!Encryptor.correctKey) {
                ClipData clip = clipboard.getPrimaryClip();
                try {
                    String clipboardString = clip.getItemAt(0).coerceToText(this).toString().trim();
                    try {
                        new BigInteger(clipboardString);
                        Encryptor.privateKey = clipboardString;
                        if (Encryptor.checkKeys(myPublicKey)) {
                            menu.findItem(R.id.privateKeyInput).setTitle("Remove Private Key");
                            Encryptor.correctKey = true;
                            ClipData emptyClip = ClipData.newPlainText("", "");
                            clipboard.setPrimaryClip(emptyClip);
                            setMenuKeyIcon(true);
                            lockSound();
                            Toast.makeText(this, "Messages Successfully Decrypted", Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                        } else {
                            vibrate();
                            Toast.makeText(this, "Clipboard private key is incorrect", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Encryptor.privateKey = "1";
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
                lockSound();
                Toast.makeText(this, "Messages are now Encrypted", Toast.LENGTH_SHORT).show();
                Encryptor.correctKey = false;
                setMenuKeyIcon(false);
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
