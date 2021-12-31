package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.databinding.ActivitySignupBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import models.UserModel;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding activitySignupBinding;
    private FirebaseAuth myAuth;
    FirebaseDatabase firebaseDatabase;
    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        if(myAuth.getCurrentUser()!=null){
            Intent intent = new Intent(SignupActivity.this,MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        }

        activitySignupBinding = ActivitySignupBinding.inflate(getLayoutInflater());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(activitySignupBinding.getRoot());

        activitySignupBinding.progressBar.setVisibility(View.GONE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        activitySignupBinding.signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email =  activitySignupBinding.mail.getText().toString().trim();
                String password = activitySignupBinding.password.getText().toString().trim();
                String userName = activitySignupBinding.username.getText().toString().trim();
                String about = "online";

                if(!email.isEmpty() && !password.isEmpty() && !userName.isEmpty())
                    signupUser(email,password, userName,about);
                else
                    Toast.makeText(SignupActivity.this, "Enter details", Toast.LENGTH_SHORT).show();
            }
        });

        activitySignupBinding.hidePassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(activitySignupBinding.password.getTransformationMethod()!=null)
                    activitySignupBinding.password.setTransformationMethod(null);
                else activitySignupBinding.password.setTransformationMethod(new PasswordTransformationMethod());

            }
        });

        activitySignupBinding.moveToSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this,SigninActivity.class);
                startActivity(intent);
            }
        });


        //      Signin with google

        activitySignupBinding.googleSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                signInWithGoogle();
                activitySignupBinding.progressBar.setVisibility(View.VISIBLE);

                        Intent signInIntent  = mGoogleSignInClient.getSignInIntent();
                        activityResultLauncher.launch(signInIntent);



            }
        });


        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
                activitySignupBinding.progressBar.setVisibility(View.GONE);

                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());

                    } catch (ApiException e) {
                        // Google Sign In failed, update UI appropriately
                        Toast.makeText(SignupActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        Log.w("TAG", "Google sign in failed", e);
                    }


            }
        });

    }



    public void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        myAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        activitySignupBinding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(SignupActivity.this,MainActivity.class);
                            startActivity(intent);
                            String id =  task.getResult().getUser().getUid();
                            String defaultUserName = task.getResult().getUser().getEmail();
                            String about = "Online";
                            firebaseDatabase.getReference().child("Users").child(id).setValue(new UserModel(defaultUserName.substring(0,defaultUserName.indexOf('@'))
                                                                                                            ,task.getResult().getUser().getEmail()
                                                                                                            ,"null"
                                                                                                            ,task.getResult().getUser().getPhotoUrl().toString()
                                                                                                            ,about));


                        } else {
                            Toast.makeText(SignupActivity.this, task.getException().getLocalizedMessage()+"", Toast.LENGTH_SHORT).show();
                            Log.d("TAG", "signInWithCredential:failure", task.getException());
                            activitySignupBinding.progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }



    private void signupUser(String email, String password, String userName, String about){

        activitySignupBinding.progressBar.setVisibility(View.VISIBLE);


        myAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        activitySignupBinding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {

                           String id =  task.getResult().getUser().getUid();

                            Intent intent = new Intent(SignupActivity.this,MainActivity.class);
                            startActivity(intent);
                            firebaseDatabase.getReference().child("Users").child(id).setValue(new UserModel(userName,email,password,"R.drawable.user",about));
                        } else {
                            Toast.makeText(SignupActivity.this, "SignUp failed "+task.getException().getLocalizedMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }




}