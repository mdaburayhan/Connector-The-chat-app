package com.arsoftltd.connector.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.arsoftltd.connector.activities.authentication.SignInActivity;
import com.arsoftltd.connector.activities.feeds.FeedActivity;
import com.arsoftltd.connector.activities.settings.SettingsActivity;
import com.arsoftltd.connector.adapters.RecentConversationsAdapter;
import com.arsoftltd.connector.databinding.ActivityMainBinding;
import com.arsoftltd.connector.listeners.ConversionListener;
import com.arsoftltd.connector.models.ChatMessage;
import com.arsoftltd.connector.models.User;
import com.arsoftltd.connector.utilities.Constants;
import com.arsoftltd.connector.utilities.PreferenceManager;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private AdView adView;
    private String blueBadgeStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();
        loadFbAds();


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

    //  facebook ads implement
    private void loadFbAds(){
        adView = new AdView(this, "IMG_16_9_APP_INSTALL#252933117323211_252939560655900", AdSize.BANNER_HEIGHT_50);
        binding.faceBookBannerAdsLayout.addView(adView);
        adView.loadAd();
    }
    private void init() {
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationRecycleView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners(){
        binding.imageSettings.setOnClickListener(v ->
                //signOut()
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class))
        );
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UsersActivity.class)));
        binding.imageProfile.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), FeedActivity.class)));
    }
    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));

        // Update user image
        String userImage = preferenceManager.getString(Constants.KEY_IMAGE);
        byte[] imageBytes = Base64.decode(userImage, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        binding.imageProfile.setImageBitmap(bitmap);

        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        String newName = preferenceManager.getString(Constants.KEY_NAME);
        String newImage = preferenceManager.getString(Constants.KEY_IMAGE);

        // Update sender name and image in conversations where the user is the sender
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshot != null) {
                        for (DocumentChange documentChange : snapshot.getDocumentChanges()) {
                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                String conversationId = documentChange.getDocument().getId();
                                HashMap<String, Object> updates = new HashMap<>();
                                updates.put(Constants.KEY_SENDER_NAME, newName);
                                updates.put(Constants.KEY_SENDER_IMAGE, newImage);

                                // Update sender name and image in conversation document
                                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                        .document(conversationId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            // Update successful
                                        })
                                        .addOnFailureListener(e -> {
                                            // Update failed
                                        });
                            }
                        }
                    }
                });

        // Update receiver name and image in conversations where the user is the receiver
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshot != null) {
                        for (DocumentChange documentChange : snapshot.getDocumentChanges()) {
                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                String conversationId = documentChange.getDocument().getId();
                                HashMap<String, Object> updates = new HashMap<>();
                                updates.put(Constants.KEY_RECEIVER_NAME, newName);
                                updates.put(Constants.KEY_RECEIVER_IMAGE, newImage);

                                // Update receiver name and image in conversation document
                                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                        .document(conversationId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            // Update successful
                                        })
                                        .addOnFailureListener(e -> {
                                            // Update failed
                                        });
                            }
                        }
                    }
                });

        // Call blueBadgeCheck() method to check and update blue badge status
        blueBadgeCheck();
        // Retrieve the blueBadge status from PreferenceManager
        blueBadgeStatus = preferenceManager.getString(Constants.KEY_BLUE_BADGE_STATUS);
        updateBlueBadgeUI();
    }

    /** if loadUserDetails has any error this code will work instead of the code
    private void loadUserDetails(){
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0 , bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    **/
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    /* If I got any error I will add one more parenthesis before the
    value and also I will finish the parenthesis at the last of the eventListener
     */
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return;
        }
        if (value != null){
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    }else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                }else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i = 0; i < conversations.size(); i++){
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationRecycleView.smoothScrollToPosition(0);
            binding.conversationRecycleView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USER).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }


    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}