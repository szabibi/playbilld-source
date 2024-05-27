package com.example.playbilld;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

/** a BrowseShowsFragmenten vagy DiaryEntryActivity-n keresztül
 * megnyitott előadást részletező oldal**/
public class ShowPageActivity extends AppCompatActivity {

    private Models.Show selectedShow;

    private DatabaseReference mDatabase;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_page);

        username = LoginPage.user_username;
        selectedShow = BrowseShowsFragment.selectedShow;

        mDatabase = Database.makeDatabase();

        ImageButton addLogButton = findViewById(R.id.imageButtonAddLog);
        addLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ShowPageActivity.this, AddNewLogActivity.class);
                intent.putExtra("action", "create");
                startActivityForResult(intent, 111);
            }
        });

        loadShowData();
    }

    private void loadReviews() {
        // végignézi az adatbázisban az összes felhasználó összes bejegyzését, és ami
        // erre az előadásra jött létre, azt megjeleníti
        DatabaseReference mDatabase = Database.makeDatabase();
        Context context = this;
        LinearLayout main = findViewById(R.id.layoutReviews);
        main.removeAllViews();

        mDatabase.child("logs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String username = snapshot.getKey();
                            for (DataSnapshot log_snapshot : snapshot.getChildren()) {

                                if (log_snapshot.getKey().toString().equals("count")) {
                                    continue;
                                }
                                Models.Log log = log_snapshot.getValue(Models.Log.class);
                                log.setId(log_snapshot.getKey());

                                // ellenőrzi, hogy a bejegyzés a megtekintett előadáshoz van-e
                                if (log.getShow().equals(Integer.toString(selectedShow.getId()))) {

                                    if (log.getReview().isEmpty()) {
                                        continue;
                                    }

                                    ViewStub log_stub = new ViewStub(context);
                                    log_stub.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                            300));
                                    main.addView(log_stub);
                                    log_stub.setLayoutResource(R.layout.review_short);
                                    View log_inflated_view = log_stub.inflate();

                                    TextView review_username = log_inflated_view.findViewById(R.id.textViewReviewUsername);
                                    RatingBar rating = log_inflated_view.findViewById(R.id.ratingBarReviewRating);
                                    TextView review_body = log_inflated_view.findViewById(R.id.textViewReviewReview);

                                    review_username.setText(username);

                                    if (log.getRating() > 0) {
                                        rating.setRating((float) log.getRating());
                                    } else {
                                        rating.setVisibility(View.GONE);
                                    }

                                    // szöveges vélemény rövidítése
                                    String reviewText = log.getReview();
                                    if(reviewText.length() > 90) {
                                        reviewText = reviewText.substring(0, 90) + "...";
                                    }

                                    review_body.setText(reviewText);
                                    ViewGroup.LayoutParams params = review_body.getLayoutParams();
                                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                    review_body.setLayoutParams(params);

                                    // a bejegyzés előnézetét kattinthatóvá is teszi,
                                    // ami a részletes naplóbeejgyzés oldalra vezet
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
                                                Intent intent = new Intent(ShowPageActivity.this, DiaryEntryActivity.class);
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
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }

                });
    }

    private void loadShowData() {
        // feltölti az activity elemeit az adott előadás adataival és képével

        Context context = this;

        ImageView poster = findViewById(R.id.imageViewPoster);
        TextView title = findViewById(R.id.textViewTitle);
        TextView premiere = findViewById(R.id.textViewPremiereDate);
        TextView music_by = findViewById(R.id.textViewMusicBy);

        Glide.with(context)
                .load(selectedShow.getImgsrc())
                .into(poster);

        title.setText(selectedShow.getTitle());
        premiere.setText(selectedShow.getPremiereString());
        String music_by_full = getResources().getString(R.string.show_page_music_by) + " | " + selectedShow.getMusic_by();
        music_by.setText(music_by_full);


        String averageRatingText = "★ " + selectedShow.getAverage();

        TextView averageRating = findViewById(R.id.textViewAverageRating);
        averageRating.setText(averageRatingText);


    }

    @Override
    protected void onResume() {
        // újra betölti az értékeléseket, hátha lett létrehozva új, és az átlagot is betölti újra

        super.onResume();

        loadReviews();

        mDatabase.child("shows").child(Integer.toString(selectedShow.getId())).child("average").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float average = snapshot.getValue(float.class);
                String averageRatingText = "★ " + average;

                TextView averageRating = findViewById(R.id.textViewAverageRating);
                averageRating.setText(averageRatingText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}