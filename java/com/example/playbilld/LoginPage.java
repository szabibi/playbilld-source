package com.example.playbilld;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class LoginPage extends AppCompatActivity {

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    private FirebaseAuth mAuth;

    EditText fieldUsername;
    EditText fieldPassword;
    AppCompatButton loginGoogleButton;

    Button loginButton;

    private DatabaseReference mDatabase;

    static public String user_username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("szabiloginpage", "login page");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginpage);

        mDatabase = Database.makeDatabase();

        fieldUsername = findViewById(R.id.editTextUsername);
        fieldPassword = findViewById(R.id.editTextPassword);

        loginButton = findViewById(R.id.buttonLogin);
        mAuth = FirebaseAuth.getInstance();

        loginGoogleButton = findViewById(R.id.buttonLoginGoogle);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        loginGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginGoogle();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitLogin();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // ha google-val vagy email-jelszó párossal van bejelentkezett felhasználó,
        // tovább irányít a főoldalra

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            getUserThenLogin(currentUser.getEmail());
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            getUserThenLogin(account.getEmail());
        }
    }

    public void goToSignup(View view) {
        Intent myIntent = new Intent(LoginPage.this, SignupActivity.class);
        startActivity(myIntent);
    }

    public void submitLogin() {
        String username = fieldUsername.getText().toString();
        String password = fieldPassword.getText().toString();

        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            getUserThenLogin(user.getEmail());
                        } else {
                            Toast.makeText(LoginPage.this, getResources().getString(R.string.login_authentication_failed),
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(Object o) {
    }

    public void loginGoogle() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1000 = Google-s bejelentkezés
        if(requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                task.getResult(ApiException.class);

                GoogleSignInAccount user = GoogleSignIn.getLastSignedInAccount(this);

                getUserThenLogin(user.getEmail());
            } catch (ApiException e) {
            }
        }
    }

    private void navigateToSecondActivity() {
        finish();

        Intent myIntent = new Intent(LoginPage.this, MainActivity.class);
        startActivity(myIntent);
    }

    private void getUserThenLogin(String email) {
        // mielőtt a főoldalra irányítja a felhasználót sikeres bejelentkezés esetén,
        // az adott e-mail címhez kikeresi a felhasználónevet
        mDatabase.child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean emailFound = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Models.User this_user = snapshot.getValue(Models.User.class);
                            if (this_user.getEmail().equals(email)) {
                                user_username = snapshot.getKey().toString();
                                emailFound = true;
                                Log.d("szabi", "username found");
                                navigateToSecondActivity();
                            }
                        }
                        if (!emailFound) {
                            // ez akkor történik, ha google-val először jelentkezik be felhasználó,
                            // és még nincs felhasználóneve
                            // átmegy a felhasználó-megadás activity-re
                            user_username = "notfound";
                            finish();
                            Intent myIntent = new Intent(LoginPage.this, CompleteSignupActivity.class);
                            startActivity(myIntent);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("databaseerror", "Failed to get data");
                        user_username = "error";
                    }
                });
    }
}
