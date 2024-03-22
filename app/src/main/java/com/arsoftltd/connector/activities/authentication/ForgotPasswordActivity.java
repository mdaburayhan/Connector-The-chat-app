package com.arsoftltd.connector.activities.authentication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.arsoftltd.connector.databinding.ActivityForgotPasswordBinding;
import com.arsoftltd.connector.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;


public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    private PreferenceManager preferenceManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firebaseAuth = FirebaseAuth.getInstance();
        setListener();
    }
    // This function will help you to manage all the Listener
    private void setListener(){
        binding.btnReset.setOnClickListener(v -> {
            resetPassword();
        });
    }
    private void resetPassword(){
        if (!isValidEmail()) {
            showToast("Enter a valid email address");
            return;
        }else{
            loading(true);
            String email = binding.inputEmail.getText().toString().trim();
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showToast("Password reset email sent. Please check your inbox/spam.");
                            startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class));
                            finish();
                        } else {
                            showToast("Failed to send password reset email. Please try again.");
                        }
                        loading(false);
                    });
        }
    }

    // boolean to check the email field empty/valid or not?
    private boolean isValidEmail() {
        String email = binding.inputEmail.getText().toString().trim();
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


    // control the progressbar
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.btnReset.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.btnReset.setVisibility(View.VISIBLE);
        }
    }




    // To control the toast functionality
    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

    }


}