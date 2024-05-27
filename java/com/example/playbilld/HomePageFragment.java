package com.example.playbilld;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomePageFragment extends Fragment {
    private String username;

    View rootView;
    Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_home_page, container, false);
        activity = getActivity();

        // bejelentkezett felhasználó username-ének átvétele
        username = LoginPage.user_username;

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFollowingActivity();
    }

    private void loadFollowingActivity() {
        DatabaseReference mDatabase = Database.makeDatabase();
        Context context = activity;
        LinearLayout main = rootView.findViewById(R.id.layoutFollowingActivity);
        main.removeAllViews();

        List<String> followedUsernames = new ArrayList<>();

        // követett felhasználók lekérése
        mDatabase.child("users").child(username).child("follows")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if(!snapshot.exists()) {
                        // nem követ senkit
                        return;
                    }

                    for(DataSnapshot s : snapshot.getChildren()) {
                        if (s.getValue(boolean.class)) {
                            followedUsernames.add(s.getKey());
                        }
                    }

                    // ha ide eljut a kód, akkor létezett a "follows" tulajdonság,
                    // de attól még lehetett az összes felhasználó benne "false"-ra állítva
                    // tehát ha üres a lista, akkor sem követ senkit
                    // ez alapján megjeleníti vagy elrejti a "kövess valakit!" üzenetet
                    TextView followSomeoneMessage = rootView.findViewById(R.id.textViewFollowingActivityMessage);
                    if (followedUsernames.isEmpty()) {
                        followSomeoneMessage.setVisibility(View.VISIBLE);
                    } else {
                        followSomeoneMessage.setVisibility(View.GONE);
                    }

                    // minden felhasználóra megkeresi a bejegyzések számát,
                    // majd az ezzel megegyező id-jű bejegyzést lekéri adatbázisból
                    // és kirajzolja azt
                    for(String u : followedUsernames) {
                        mDatabase.child("logs").child(u).child("count")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        return;
                                    }

                                    int logCount = snapshot.getValue(int.class);

                                    mDatabase.child("logs").child(u).child(Integer.toString(logCount))
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange( DataSnapshot snapshot) {
                                                    Models.Log log = snapshot.getValue(Models.Log.class);
                                                    log.buildRest(u);
                                                    addLogToLayout(log, context, main);
                                                }

                                                @Override
                                                public void onCancelled( DatabaseError error) {

                                                }
                                            });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
    }

    private void addLogToLayout(Models.Log log, Context context, LinearLayout main) {
        ViewStub log_stub = new ViewStub(context);
        log_stub.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                300));
        main.addView(log_stub);
        log_stub.setLayoutResource(R.layout.review_short);
        View log_inflated_view = log_stub.inflate();

        TextView review_username = log_inflated_view.findViewById(R.id.textViewReviewUsername);
        RatingBar rating = log_inflated_view.findViewById(R.id.ratingBarReviewRating);
        TextView review_body = log_inflated_view.findViewById(R.id.textViewReviewReview);

        String logTitle = log.getUser() + " " + getResources().getString(R.string.watched);
        review_username.setText(logTitle);

        if (log.getRating() > 0) {
            rating.setRating((float) log.getRating());
        } else {
            rating.setVisibility(View.GONE);
        }

        // ha túl hosszú a szöveges vélemény, pont-pont-ponttal rövidíti
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

        // a bejegyzést kattinthatóvá teszi, részletek oldalra vezet
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
                    Intent intent = new Intent(activity, DiaryEntryActivity.class);
                    intent.putExtra("username", log.getUser());
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