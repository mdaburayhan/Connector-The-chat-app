package com.arsoftltd.connector.activities.settings;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.arsoftltd.connector.R;
import com.arsoftltd.connector.activities.settings.UpdateProfileActivity;
import com.arsoftltd.connector.activities.authentication.SignInActivity;
import com.arsoftltd.connector.databinding.ActivitySettingsBinding;
import com.arsoftltd.connector.utilities.Constants;
import com.arsoftltd.connector.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String blueBadgeStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        setListeners();
        loadUserDetails();


    }

    //BlueBadge status update
    private void updateBlueBadgeUI() {
        if (blueBadgeStatus != null && blueBadgeStatus.equals("true")) {
            binding.blueBadge.setVisibility(View.VISIBLE);
        } else {
            binding.blueBadge.setVisibility(View.GONE);
        }
    }

    // this function is use for handle the listener
    private void setListeners(){
        binding.buttonSignOut.setOnClickListener(v ->
                signOut());
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.editProfileSettings.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UpdateProfileActivity.class))
                );
    }

    // method to sign out the user
    private void signOut(){
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USER).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    FirebaseAuth.getInstance().signOut();
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    // this function will handle all the toast
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    //Loading current User data to show on the settings page
    private void loadUserDetails(){
        binding.textNameProfileViewSettings.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0 , bytes.length);
        binding.imageProfileViewSettings.setImageBitmap(bitmap);



        // Retrieve the blueBadge status from PreferenceManager
        blueBadgeStatus = preferenceManager.getString(Constants.KEY_BLUE_BADGE_STATUS);
        updateBlueBadgeUI();
    }

}