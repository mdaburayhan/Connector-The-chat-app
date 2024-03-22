package com.arsoftltd.connector.activities.feeds;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.arsoftltd.connector.R;
import com.arsoftltd.connector.activities.MainActivity;
import com.arsoftltd.connector.activities.UsersActivity;
import com.arsoftltd.connector.adapters.UsersAdapter;
import com.arsoftltd.connector.databinding.ActivityFeedBinding;
import com.arsoftltd.connector.listeners.UserListener;
import com.arsoftltd.connector.models.User;
import com.arsoftltd.connector.utilities.Constants;
import com.arsoftltd.connector.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedActivity extends AppCompatActivity implements UserListener {

    private ActivityFeedBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String blueBadgeStatus;

    private static final int REQUEST_IMAGE_PICK = 1;

    private ImageView imagePost;
    private List<Uri> selectedImages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());


        init();
        setListener();
        loadUserDetails();


    }


    //Initialize important and necessary things
    private void init(){
        database = FirebaseFirestore.getInstance();

    }
    // Setting up all the intent and listener here
    private void setListener(){
        binding.messageButton.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), MainActivity.class)));

        binding.fabNewPost.setOnClickListener(v ->
                showNewPostPopup());

        // On click listener to convert the search icon to edit text
        binding.searchView.setOnClickListener(v -> {
            binding.searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT);
        });
        // On editor listener to get the search option on the keyboard
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchByEmail();
                return true;
            }
            return false;
        });
    }

    //showing the pop-up new post layout
    private void showNewPostPopup(){
        // Inflate the popup_new_post.xml layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_new_post, null);

        // Create the popup window
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // Allows tapping outside the popup to dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Show the popup window
        popupWindow.showAtLocation(binding.fabNewPost, Gravity.CENTER, 0, 0);

        // Handle the click events inside the popup window
        EditText editText = popupView.findViewById(R.id.editText);
        Button postButton = popupView.findViewById(R.id.postButton);

        // create post profile image setup
        RoundedImageView imageProfileCreatePost = popupView.findViewById(R.id.imageProfileCreatePost);
        String userImage = preferenceManager.getString(Constants.KEY_IMAGE);
        byte[] imageBytes = Base64.decode(userImage, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        binding.imageProfile.setImageBitmap(bitmap);

        // set user name on post pop up layout
        String newName = preferenceManager.getString(Constants.KEY_NAME);
        TextView postTextName = popupView.findViewById(R.id.postTextName);
        postTextName.setText(newName);

        // create blue badge check
        ImageView  blueBadgeCreatePost =  popupView.findViewById(R.id.blueBadgeCreatePost);
        if (blueBadgeStatus != null && blueBadgeStatus.equals("true")) {
            blueBadgeCreatePost.setVisibility(View.VISIBLE);
        } else {
            blueBadgeCreatePost.setVisibility(View.GONE);
        }

        imageProfileCreatePost.setImageBitmap(bitmap);


        //Image pickup
        imagePost = popupView.findViewById(R.id.imagePost);
        Button selectImageButton = popupView.findViewById(R.id.selectImageButton);

        selectedImages = new ArrayList<>();

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });


        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = editText.getText().toString();
                // Perform the post operation here
                // ...

                // Dismiss the popup window
                popupWindow.dismiss();
            }
        });


    }


    //Image pickup
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    // Multiple images selected
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImages.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    // Single image selected
                    Uri imageUri = data.getData();
                    selectedImages.add(imageUri);
                }

                // Display the selected images
                displaySelectedImages();
            }
        }
    }

    // Image preview on create new post
    private void displaySelectedImages() {
        if (selectedImages.size() > 0) {
            // Show the first selected image in the ImageView
            Uri firstImageUri = selectedImages.get(0);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), firstImageUri);
                imagePost.setImageBitmap(bitmap);
                imagePost.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // Loading user personal data
    private void loadUserDetails(){
        String userImage = preferenceManager.getString(Constants.KEY_IMAGE);
        byte[] imageBytes = Base64.decode(userImage, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        binding.imageProfile.setImageBitmap(bitmap);


        // Call blueBadgeCheck() method to check and update blue badge status
        blueBadgeCheck();

        // Retrieve the blueBadge status from PreferenceManager
        blueBadgeStatus = preferenceManager.getString(Constants.KEY_BLUE_BADGE_STATUS);
        updateBlueBadgeUI();

    }
    //Blue badge Program
    private void blueBadgeCheck() {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        // Retrieve user document from Firestore
        database.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String blueBadgeText = documentSnapshot.getString("blueBadge");
                        // Use the blueBadgeText as needed
                        blueBadgeStatus = blueBadgeText;
                        // Save the blueBadge status in PreferenceManager
                        preferenceManager.putString(Constants.KEY_BLUE_BADGE_STATUS, blueBadgeStatus);
                        updateBlueBadgeUI();
                    } else {
                        // User document not found, set blueBadgeStatus to null
                        blueBadgeStatus = null;
                        // Save the blueBadge status in PreferenceManager
                        preferenceManager.putString(Constants.KEY_BLUE_BADGE_STATUS, null);
                        updateBlueBadgeUI();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors that occurred
                });
    }

    //Blue badge Program
    private void updateBlueBadgeUI() {
        if (blueBadgeStatus != null && blueBadgeStatus.equals("true")) {
            binding.blueBadge.setVisibility(View.VISIBLE);
        } else {
            binding.blueBadge.setVisibility(View.GONE);
        }
    }



    // Creating a new function called performSearch() to make the functionality of performSearch
    private void searchByEmail(){
        String searchQuery = binding.searchEditText.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USER)
                    .whereEqualTo(Constants.KEY_EMAIL, searchQuery)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<User> users = new ArrayList<>();
                            for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                User user = new User();
                                user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                user.id = queryDocumentSnapshot.getId();
                                users.add(user);
                            }
                            if (!users.isEmpty()) {
                                UsersAdapter usersAdapter = new UsersAdapter(users, this);
                                binding.feedRecycleView.setAdapter(usersAdapter);
                                binding.feedRecycleView.setVisibility(View.VISIBLE);
                                binding.textErrorMessage.setVisibility(View.GONE);
                            } else {
                                searchByName(searchQuery);
                            }
                        } else {
                            searchByName(searchQuery);
                        }
                    });
        }
    }

    // Creating a new function called searchByName to search user by their name
    private void searchByName(String searchQuery){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .orderBy(Constants.KEY_NAME)
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if (users.isEmpty()) {
                            searchByCharacter(searchQuery);
                        } else {
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.feedRecycleView.setAdapter(usersAdapter);
                            binding.feedRecycleView.setVisibility(View.VISIBLE);
                            binding.textErrorMessage.setVisibility(View.GONE);
                        }
                    } else {
                        searchByCharacter(searchQuery);
                    }
                });
    }

    // Creating a new function called searchByCharacter to search user by character
    private void searchByCharacter(String searchQuery) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .orderBy(Constants.KEY_NAME)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        String lowercaseQuery = searchQuery.toLowerCase(); // Convert search query to lowercase
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            User user = queryDocumentSnapshot.toObject(User.class);
                            String lowercaseName = user.getName().toLowerCase(); // Convert user's name to lowercase
                            if (lowercaseName.contains(lowercaseQuery)) { // Perform case-insensitive comparison
                                user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                user.id = queryDocumentSnapshot.getId();
                                users.add(user);
                            }
                        }
                        UsersAdapter usersAdapter = new UsersAdapter(users, this);
                        binding.feedRecycleView.setAdapter(usersAdapter);
                        binding.feedRecycleView.setVisibility(View.VISIBLE);
                        if (users.isEmpty()) {
                            showSearchErrorMessage();
                        } else {
                            binding.textErrorMessage.setVisibility(View.GONE);
                        }
                    } else {
                        showSearchErrorMessage();
                    }
                });
    }

    //error message when there is no searched matched account
    private void showSearchErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s", "No user available, try using user mail account"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
        binding.feedRecycleView.setVisibility(View.INVISIBLE);
    }




    // Handel after clicking on search value
    @Override
    public void onUserClicked(User user) {

    }
}