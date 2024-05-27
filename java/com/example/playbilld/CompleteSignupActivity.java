package com.example.playbilld;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

// ez az Activity akkor indul el, ha a felhasználó új google fiókkal próbál belépni,
// amihez így még nem volt megadva felhasználónév
// az Activity során megteszi ezt
public class CompleteSignupActivity extends AppCompatActivity {

    // bejelentkezett felhasználó lekéréséhez kell
    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    GoogleSignInAccount account;

    private DatabaseReference mDatabase;

    Button cancelSignupButton;
    Button completeSignupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_signup);

        account = GoogleSignIn.getLastSignedInAccount(this);

        cancelSignupButton = findViewById(R.id.buttonCancel);
        cancelSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelSignup();
            }
        });

        completeSignupButton = findViewById(R.id.buttonComplete);
        completeSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeSignup();
            }
        });

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        mDatabase = Database.makeDatabase();
    }

    private void completeSignup() {
        EditText fieldUsername = findViewById(R.id.editTextUsername);
        String username = fieldUsername.getText().toString();
        Log.d("username", username);
        String email = account.getEmail();

        //------az adatbázist végignézi, használt-e már a megadott felhasználónév-------
        mDatabase.child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean usernameIsTaken = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            //------pontosabban itt-------
                            if (snapshot.getKey().equals(username)) {
                                Toast.makeText(CompleteSignupActivity.this, "This username is taken already",
                                        Toast.LENGTH_LONG).show();
                                usernameIsTaken = true;
                                break;
                            }
                        }


                        //------ha nem, akkor regisztrálja a firebase funkcióval az új fiókot-------
                        if (usernameIsTaken == false) {
                            Log.d("username", "not taken");
                            LoginPage.user_username = username;
                            registerNewAccount(email, username);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("databaseerror", "Failed to get data");
                    }
                });
    }

    private void navigateToHomePage(String username) {
        LoginPage.user_username = username;
        finish();
        Intent myIntent = new Intent(CompleteSignupActivity.this, MainActivity.class);
        startActivity(myIntent);
    }

    private void registerNewAccount(String email, String username) {
        // a felhasználónevet az adatbázisba külön eltárolja
        Models.User newUser = new Models.User(email, true);
        mDatabase.child("users").child(username).setValue(newUser);

        Log.d("userdata", newUser.toString());

        Toast.makeText(CompleteSignupActivity.this, getResources().getString(R.string.signup_success),
                Toast.LENGTH_SHORT).show();

        navigateToHomePage(username);
    }

    public void cancelSignup() {
        // amennyiben a mégse gombot nyomja meg a felhasználó, ki kell jelentkeztetni

        FirebaseAuth.getInstance().signOut();

        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                finish();
                startActivity(new Intent(CompleteSignupActivity.this, LoginPage.class));
            }
        });
    }
}