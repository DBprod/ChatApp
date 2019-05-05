package com.example.chatapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SettingActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;
    File localFile = null;
    private StorageReference storageref = FirebaseStorage.getInstance().getReference();
    int ayyyy = 0;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingContainer, new SettingFrame())
                .commit();


        Intent adsf = getIntent();
        uid = adsf.getStringExtra("uid");
        final ImageView profilePic = findViewById(R.id.imageView4);
        myRef = database.getReference("Users/" + uid + "/image");//replace with users/uid/image
        Picasso.with(this).load(R.drawable.avatar_icon).resize(70,70).centerCrop().into(profilePic, new Callback() {
            @Override
            public void onSuccess() {
                Bitmap imgbmap = ((BitmapDrawable)profilePic.getDrawable()).getBitmap();
                RoundedBitmapDrawable imgDrawable = RoundedBitmapDrawableFactory.create(getResources(), imgbmap);
                imgDrawable.setCircular(true);
                imgDrawable.setCornerRadius(35);
                profilePic.setImageDrawable(imgDrawable);
            }
            @Override
            public void onError() {
            }
        });



        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(ayyyy==1){
                    ayyyy=0;
                    return;
                }
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                try {
                    localFile = File.createTempFile("images", "jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                storageref.child("images/"+uid+".jpg").getFile(localFile) // replace fdsa with uid
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                // Successfully downloaded data to local file
                                // ...
                                Picasso.with(SettingActivity.this).load(localFile).resize(70,70).centerCrop().into(profilePic, new Callback() {
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
    }




    public void onSelectClicked(View view){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                Uri selectedUri = data.getData();
                final String path = getPathFromURI(selectedUri);
                if (path != null) {
                    File f = new File(path);
                    selectedUri = Uri.fromFile(f);
                }
                final ImageView img = findViewById(R.id.imageView4);
                final Uri finalSelectedUri = selectedUri;
//                final TextView fdsa = findViewById(R.id.textView7);
                Picasso.with(this).load(selectedUri).resize(70,70).centerCrop().into(img, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap imgbmap = ((BitmapDrawable)img.getDrawable()).getBitmap();
                        RoundedBitmapDrawable imgDrawable = RoundedBitmapDrawableFactory.create(getResources(), imgbmap);
                        imgDrawable.setCircular(true);
                        imgDrawable.setCornerRadius(35);
                        img.setImageDrawable(imgDrawable);
                        ayyyy=1;
                        StorageReference imgRef = storageref.child("images/"+uid+".jpg");//replace fdsa with uid;

                        imgRef.putFile(finalSelectedUri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                        // Get a URL to the uploaded content
//                                            Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                                            myRef.setValue(taskSnapshot.getUploadSessionUri().toString());
//                                            fdsa.setText(taskSnapshot.getUploadSessionUri().toString());
                                        }



                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception exception) {
//                                        fdsa.setText(exception.getMessage());
                                        // Handle unsuccessful uploads
                                        // ...
                                    }
                                });

                    }
                    @Override
                    public void onError() {
                    }
                });
            }
        } catch (Exception e) {
        }
    }
    public String getPathFromURI(Uri imgUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(imgUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }
}
