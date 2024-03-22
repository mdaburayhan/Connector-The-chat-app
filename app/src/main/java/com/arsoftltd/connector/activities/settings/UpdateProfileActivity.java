package com.arsoftltd.connector.activities.settings;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.arsoftltd.connector.R;
import com.arsoftltd.connector.databinding.ActivityUpdateProfileBinding;
import com.arsoftltd.connector.utilities.Constants;
import com.arsoftltd.connector.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {
    private ActivityUpdateProfileBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private boolean toastShown = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        loadUserDetails();
        setListener();

    }
    private void setListener(){
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        binding.buttonUpdate.setOnClickListener(v -> {
            if (isValidUpdateDetails()) {
                updateUserData();
            }
        });
    }


    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK){
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    // set the default user data
    private void loadUserDetails() {
        String name = preferenceManager.getString(Constants.KEY_NAME);
        String email = preferenceManager.getString(Constants.KEY_EMAIL);
        String gender = preferenceManager.getString(Constants.KEY_GENDER);

        if (gender != null && !gender.isEmpty()) {
            // Find the corresponding radio button based on the gender value
            if (gender.equalsIgnoreCase(getString(R.string.male))) {
                binding.radioButtonMale.setChecked(true);
            } else if (gender.equalsIgnoreCase(getString(R.string.female))) {
                binding.radioButtonFemale.setChecked(true);
            } else if (gender.equalsIgnoreCase(getString(R.string.other))) {
                binding.radioButtonOther.setChecked(true);
            }
        }

        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0 , bytes.length);

        binding.imageProfile.setImageBitmap(bitmap);
        binding.editTextName.setText(name);
        binding.editTextEmail.setText(email);

        // Fetch and set the email from the database
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId != null) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            DocumentReference userRefUpdate = database.collection(Constants.KEY_COLLECTION_USER)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID));

            userRefUpdate.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userEmail = documentSnapshot.getString(Constants.KEY_EMAIL);
                    String userGender = documentSnapshot.getString(Constants.KEY_GENDER);


                    if (userEmail != null && !userEmail.isEmpty()) {
                        binding.editTextEmail.setText(userEmail);
                        // Update the value of KEY_EMAIL in the preference manager
                        preferenceManager.putString(Constants.KEY_EMAIL, userEmail);
                    }
                    if (userGender != null && !userGender.isEmpty()) {
                        // Find the corresponding radio button based on the user's gender
                        if (userGender.equalsIgnoreCase(getString(R.string.male))) {
                            binding.radioButtonMale.setChecked(true);
                        } else if (userGender.equalsIgnoreCase(getString(R.string.female))) {
                            binding.radioButtonFemale.setChecked(true);
                        } else if (userGender.equalsIgnoreCase(getString(R.string.other))) {
                            binding.radioButtonOther.setChecked(true);
                        }
                    }
                }
            }).addOnFailureListener(exception -> {
                // Handle failure to retrieve data from the database
            });
        }
    }

    private void updateUserData() {
        // Retrieve the user's current password from the user input
        String password = binding.editTextPassword.getText().toString().trim();

        // Reauthenticate the user before updating the email
        reauthenticateUser(password);
    }


    // how to re-authenticate user for updating email

    private void reauthenticateUser(String password) {

        if (password.isEmpty()) {
            showToast("Please enter your password.");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        // User has been successfully reauthenticated, proceed with updating the email

                        FirebaseFirestore database = FirebaseFirestore.getInstance();
                        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

                        if (userId != null) {
                            DocumentReference userRef = database.collection(Constants.KEY_COLLECTION_USER).document(userId);

                            // To Update the user current photo with the database photo
                            DocumentReference userRefUpdate = database.collection(Constants.KEY_COLLECTION_USER)
                                    .document(preferenceManager.getString(Constants.KEY_USER_ID));

                            userRefUpdate.get().addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String encodedImage = documentSnapshot.getString(Constants.KEY_IMAGE);

                                    if (encodedImage != null && !encodedImage.isEmpty()) {
                                        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        binding.imageProfile.setImageBitmap(bitmap);
                                        binding.textAddImage.setVisibility(View.GONE);

                                        // Update the value of KEY_IMAGE in the preference manager
                                        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                                    } else {
                                        // If there is no image available, you can set a placeholder image
                                        binding.imageProfile.setImageResource(R.drawable.ic_person);
                                        binding.textAddImage.setVisibility(View.VISIBLE);

                                        // Update the value of KEY_IMAGE in the preference manager to null or empty
                                        preferenceManager.putString(Constants.KEY_IMAGE, "");
                                    }
                                }
                            }).addOnFailureListener(exception -> {
                                // Handle failure to retrieve data from the database
                            });
                            ////////////////////

                            String name = binding.editTextName.getText().toString().trim();
                            String email = binding.editTextEmail.getText().toString().trim();
                            String gender = "";

                            int selectedRadioButtonId = binding.radioGroupGender.getCheckedRadioButtonId();
                            if (selectedRadioButtonId != -1) {
                                RadioButton selectedRadioButton = findViewById(selectedRadioButtonId);
                                gender = selectedRadioButton.getText().toString();
                            }

                            final String finalGender = gender; // Declare final variable for gender

                            Map<String, Object> updates = new HashMap<>();
                            updates.put(Constants.KEY_NAME, name);
                            updates.put(Constants.KEY_EMAIL, email);
                            updates.put(Constants.KEY_GENDER, finalGender); // Use the finalGender variable

                            // Check if an image is selected
                            if (encodedImage != null) {
                                updates.put(Constants.KEY_IMAGE, encodedImage); // Add the encoded image to the updates
                            }


                            userRef.update(updates)
                                    .addOnSuccessListener(aVoid1 -> {
                                        preferenceManager.putString(Constants.KEY_NAME, name);
                                        preferenceManager.putString(Constants.KEY_EMAIL, email);
                                        preferenceManager.putString(Constants.KEY_GENDER, finalGender); // Use the finalGender variable


                                        // Show the toast message only if it hasn't been shown before
                                        if (!toastShown) {
                                            showToast("Your information has been updated successfully");
                                            toastShown = true; // Set the flag to true to indicate that the toast has been shown
                                        }

                                        //Calling the updateFirebaseAuthEmail to update the mail in firebase authentication
                                        updateFirebaseAuthEmail(email);

                                        finish();
                                    })
                                    .addOnFailureListener(exception -> showToast("Failed to update your data. Please try again."));
                        }

                    })
                    .addOnFailureListener(exception -> showToast("Please check your password."));

        }
        else {
            String logInEmail = preferenceManager.getString(Constants.KEY_EMAIL);

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mAuth.signInWithEmailAndPassword(logInEmail, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                showToast("Please update once more to confirm");

                            } else {
                               showToast("Please check your password");
                            }
                        }
                    });
        }
    }



    // this method is using to update the firebase authentication email. I don't know will it work or not...
    private void updateFirebaseAuthEmail(String email) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is signed in
            currentUser.updateEmail(email)
                    .addOnSuccessListener(aVoid -> {
                        showToast("Your information has been updated successfully");
                        // After updating the Firebase Authentication email,
                        // call the method to update the Firestore email as well.
                    })
                    .addOnFailureListener(exception -> showToast("Failed to update your information. Please try again."));

        } else {
            // User is not signed in
            showToast("Something went wrong.");
        }
    }




    private Boolean isValidUpdateDetails() {
        if (binding.editTextName.getText().toString().trim().isEmpty()) {
            showToast("Enter Name");
            return false;
        } else if (binding.editTextEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
