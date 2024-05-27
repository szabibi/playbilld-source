package com.example.playbilld;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;

    AppCompatButton signupGoogleButton;
    private FirebaseAuth mAuth;

    private DatabaseReference mDatabase;

    EditText fieldUsername;
    EditText fieldEmail;
    EditText fieldPassword;
    EditText fieldPasswordAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        fieldUsername = findViewById(R.id.editTextUsername);
        fieldEmail = findViewById(R.id.editTextEmail);
        fieldPassword = findViewById(R.id.editTextPassword);
        fieldPasswordAgain = findViewById(R.id.editTextPasswordAgain);

        signupGoogleButton = findViewById(R.id.buttonSignupGoogle);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        signupGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signupGoogle();
            }
        });

        mAuth = FirebaseAuth.getInstance();

        mDatabase = Database.makeDatabase();
    }

    public void goToLogin(View view) {
        Intent myIntent = new Intent(SignupActivity.this, LoginPage.class);
        startActivity(myIntent);
    }

    public void submitSignup(View view) {
        String password = fieldPassword.getText().toString();
        String password_again = fieldPasswordAgain.getText().toString();
        String username = fieldUsername.getText().toString();
        String email = fieldEmail.getText().toString();

        // le kell futtatni egy csomó tesztet, hogy érvényes fiók készül-e
        mDatabase.child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean emailFound = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Models.User user = snapshot.getValue(Models.User.class);
                            if (user.getEmail().equals(email)) {
                                emailFound = true;
                                break;
                            }
                        }

                        // ha még nem létezik az email címen fiók...
                        if (!emailFound) {
                            //---------üres mező---------
                            if (username.isEmpty() || password.isEmpty() || password_again.isEmpty()) {
                                Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_fill_all_fields),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            //----------túl rövid jelszó
                            if (password.length() < 8) {
                                Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_eight_or_longer),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            //----------nem egyező jelszavak----------
                            if (!password.equals(password_again)) {
                                Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_passwords_dont_match),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            //------felhasználónév foglaltságának ellenőrzése-------
                            mDatabase.child("users")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            boolean usernameIsTaken = false;
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                //------itt ellenőriz-------
                                                if (snapshot.getKey().equals(username)) {
                                                    Toast.makeText(SignupActivity.this, getResources().getString(R.string.username_taken),
                                                            Toast.LENGTH_LONG).show();
                                                    usernameIsTaken = true;
                                                    break;
                                                }
                                            }

                                            //------ha nem talált ugyanilyet, regisztrálja a fiókot-------
                                            if (!usernameIsTaken) {
                                                registerNewAccount(email, username, password);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                        }
                                    });
                        } else {
                            Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_email_exists),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    private void registerNewAccount(String email, String username, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // a felhasználónevet külön kell tárolni az adatbázisban
                            Models.User newUser = new Models.User(email, false);
                            mDatabase.child("users").child(username).setValue(newUser);

                            Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_success),
                                    Toast.LENGTH_SHORT).show();

                            navigateToHomePage(username);
                        } else {
                            // hibaüzenet
                            Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_fail),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // lényegében a google-s regisztráció is googles-s bejelentkezés,
    // ugyanúgy le kell tesztelni, hogy volt-e korábban megadva felhasználónév
    // ha volt, de nem google-s a fiók, akkor nem enged a főoldalra (kijelentkezik),
    // és közli a felhasználóval
    // ha volt megadva felhasználónév, de google-s regisztráció során, akkor csak szimplán
    // a főoldalra irányít
    public void signupGoogle() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                task.getResult(ApiException.class);

                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

                mDatabase.child("users")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                boolean emailFound = false;
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    Models.User user = snapshot.getValue(Models.User.class);
                                    if (user.getEmail().equals(account.getEmail())) {
                                        emailFound = true;
                                        if (user.isGoogle()) {
                                            String username = snapshot.getKey();
                                            navigateToHomePage(username);
                                        } else {
                                            Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_email_exists),
                                                    Toast.LENGTH_LONG).show();
                                            signOut();
                                        }
                                        break;
                                    }
                                }
                                if (!emailFound) {
                                    navigateToSignupGoogle();
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                signOut();
                            }
                        });
            } catch (ApiException e) {
                signOut();
            }
        }
    }

    private void signOut() {
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
            }
        });
    }

    private void navigateToSignupGoogle() {
        finish();
        Intent myIntent = new Intent(SignupActivity.this, CompleteSignupActivity.class);
        startActivity(myIntent);
    }

    private void navigateToHomePage(String username) {
        LoginPage.user_username = username;
        finish();
        Intent myIntent = new Intent(SignupActivity.this, MainActivity.class);
        startActivity(myIntent);
    }
}