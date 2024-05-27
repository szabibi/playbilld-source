package com.example.playbilld;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserProfile extends AppCompatActivity {

    private String username;

    private boolean isUserMe = false;

    private Button buttonFollow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Get the Intent that started this activity
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("username")) {
            username = intent.getStringExtra("username");
            TextView textViewUsername = findViewById(R.id.textViewUsername);
            textViewUsername.setText(username);

            if (username.equals(LoginPage.user_username)) {
                isUserMe = true;
            }
        }

        // ha saját profil van megnyitva, a követés gombot eltünteti
        // különben ellátja a funkciójával
        buttonFollow = findViewById(R.id.buttonFollow);
        if (isUserMe) {
            buttonFollow.setVisibility(View.GONE);
        } else {
            buttonFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow();
                }
            });
            checkIfFollow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // a legutóbbi bejegyzések törlés és szerkesztés által, illetve
        // a követett felhasználók folyamatosan változhatnak, így ezeket
        // resume-kor olvassa be
        loadFollowingUsers();
        loadLatestLogs(3);
    }

    private void loadFollowingUsers() {
        DatabaseReference mDatabase = Database.makeDatabase();
        Context context = this;
        LinearLayout main = findViewById(R.id.linearLayoutFollowing);
        main.removeAllViews();

        final List<String> usernames = new ArrayList<>();

        mDatabase.child("users").child(username).child("follows")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.getValue(boolean.class)) {
                                usernames.add(snapshot.getKey());
                            }
                        }

                        if (!usernames.isEmpty()) {
                            TextView textNone = findViewById(R.id.textViewFollowingNone);
                            textNone.setVisibility(View.GONE);

                        Collections.sort(usernames);

                        for(String u : usernames) {
                            TextView textView = new TextView(context);
                            textView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT));

                            textView.setText(u);
                            textView.setTypeface(null, Typeface.BOLD);

                            // a követett felhasználó neve kattintható lesz
                            // az adott profilra vezet
                            textView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(UserProfile.this, UserProfile.class);
                                    intent.putExtra("username", u);
                                    startActivity(intent);
                                }
                            });

                            main.addView(textView);
                        }

                        }

                    }

                    @Override
                    public void onCancelled( DatabaseError error) {

                    }
                });
    }

    private void toggleFollow() {
        DatabaseReference mDatabase = Database.makeDatabase();
        mDatabase.child("users").child(LoginPage.user_username).child("follows").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // lehet, hogy e felhasználóhoz még nem létezik a "follows"-on belül adat,
                        // ez is azt jelenti, hogy nem követik az illetőt
                        if (!snapshot.exists() || snapshot.getValue(Boolean.class) == false) {
                            // most már követi
                            mDatabase.child("users")
                                    .child(LoginPage.user_username)
                                    .child("follows")
                                    .child(username)
                                    .setValue(true);
                            updateButtonStyle(true);
                        } else {
                            // most már nem követi
                            mDatabase.child("users")
                                    .child(LoginPage.user_username)
                                    .child("follows")
                                    .child(username)
                                    .setValue(false);
                            updateButtonStyle(false);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIfFollow() {
        // az Activity elején fut le, hogy a követés gomb megfelelően legyen formázva

        DatabaseReference mDatabase = Database.makeDatabase();
        mDatabase.child("users").child(LoginPage.user_username).child("follows").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue(Boolean.class) == true) {
                            // following
                            updateButtonStyle(true);
                        } else {
                            // not following
                            updateButtonStyle(false);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateButtonStyle(boolean following) {
        if (following) {
            buttonFollow.setText(getResources().getString(R.string.user_page_following));
            buttonFollow.setBackground(getResources().getDrawable(R.drawable.follow_button_following));
            buttonFollow.setTextColor(getResources().getColor(R.color.support));
            buttonFollow.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.check), null, null, null);
        } else {
            buttonFollow.setText(getResources().getString(R.string.user_page_follow));
            buttonFollow.setBackground(getResources().getDrawable(R.drawable.follow_button_not_following));
            buttonFollow.setTextColor(getResources().getColor(R.color.white));
            buttonFollow.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.addfriendicon), null, null, null);

        }
    }

    private void loadLatestLogs(int n) {
        DatabaseReference mDatabase = Database.makeDatabase();
        List<Models.Log> logs = new ArrayList<>();
        Context context = this;
        LinearLayout main = findViewById(R.id.logsLayout);
        main.removeAllViews();

        mDatabase.child("logs").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.getKey().equals("count")) {
                                continue;
                            }

                            Models.Log log = snapshot.getValue(Models.Log.class);
                            log.setId(snapshot.getKey());
                            logs.add(log);
                        }

                        Collections.reverse(logs);

                        for(int i = 0; i < n && i < logs.size(); i++) {
                            Models.Log log = logs.get(i);

                            ViewStub log_stub = new ViewStub(context);
                            log_stub.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                    300));
                            main.addView(log_stub);
                            log_stub.setLayoutResource(R.layout.review_short);
                            View log_inflated_view = log_stub.inflate();

                            TextView review_username = log_inflated_view.findViewById(R.id.textViewReviewUsername);
                            RatingBar rating = log_inflated_view.findViewById(R.id.ratingBarReviewRating);
                            TextView review_body = log_inflated_view.findViewById(R.id.textViewReviewReview);

                            review_username.setText(log.getShowTitle());
                            if (log.getRating() > 0) {
                                rating.setRating((float) log.getRating());
                            } else {
                                rating.setVisibility(View.INVISIBLE);
                            }

                            String reviewText = log.getReview();
                            if (reviewText.length() > 78) {
                                reviewText = reviewText.substring(0, 78) + "...";
                            }

                            review_body.setText(reviewText);

                            ImageView poster = log_inflated_view.findViewById(R.id.imageViewPoster);
                            poster.setVisibility(View.VISIBLE);

                            Glide.with(context)
                                    .load(log.getShowImgSrc())
                                    .into(poster);

                            log_inflated_view.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                        v.setBackgroundColor(getResources().getColor(R.color.light_gray));
                                        return true;
                                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                                        v.setBackground(getResources().getDrawable(R.drawable.border));
                                        return true;
                                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                                        v.setBackground(getResources().getDrawable(R.drawable.border));
                                        Intent intent = new Intent(UserProfile.this, DiaryEntryActivity.class);
                                        intent.putExtra("username", username);
                                        intent.putExtra("log", log);
                                        intent.putExtra("showId", log.getShow());
                                        startActivity(intent);
                                        return true;
                                    }
                                    return false;
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}