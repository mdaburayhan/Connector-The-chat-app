package com.arsoftltd.connector.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import com.arsoftltd.connector.adapters.UsersAdapter;
import com.arsoftltd.connector.databinding.ActivityUsersBinding;
import com.arsoftltd.connector.listeners.UserListener;
import com.arsoftltd.connector.models.User;
import com.arsoftltd.connector.utilities.Constants;
import com.arsoftltd.connector.utilities.PreferenceManager;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
        loadFbAds();
    }

    //  facebook ads implement
    private void loadFbAds(){
        adView = new AdView(this, "IMG_16_9_APP_INSTALL#252933117323211_252939560655900", AdSize.BANNER_HEIGHT_50);
        binding.faceBookBannerAdsLayout.addView(adView);
        adView.loadAd();
    }
    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());

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
                                binding.usersRecycleView.setAdapter(usersAdapter);
                                binding.usersRecycleView.setVisibility(View.VISIBLE);
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
                            binding.usersRecycleView.setAdapter(usersAdapter);
                            binding.usersRecycleView.setVisibility(View.VISIBLE);
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
                        binding.usersRecycleView.setAdapter(usersAdapter);
                        binding.usersRecycleView.setVisibility(View.VISIBLE);
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


    // Use this method to get the user on the user activity
    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }

                        // Shuffle the users randomly
                        Collections.shuffle(users);

                        if (users.size() > 0) {
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.usersRecycleView.setAdapter(usersAdapter);
                            binding.usersRecycleView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    }
                });
    }


    // Error message when there will not available no user
    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s", "No user available, try using user mail account"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }
    //error message when there is no searched matched account
    private void showSearchErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s", "No user available, try using user mail account"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
        binding.usersRecycleView.setVisibility(View.INVISIBLE);
    }

    private void loading(Boolean isLoading){
        if (isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }

}