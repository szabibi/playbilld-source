package com.example.playbilld;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class DiaryEntryActivity extends AppCompatActivity {

    private String username;
    private Models.Log log;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_entry);

        intent = getIntent();
        username = intent.getStringExtra("username");
        log = (Models.Log)intent.getSerializableExtra("log");

        Button buttonEdit = findViewById(R.id.buttonEditLog);
        Button buttonDelete = findViewById(R.id.buttonDeleteLog);

        // ha saját bejegyzést tekintünk meg, ellátja funkcióival a szerkesztés és törlés gombokat
        if (username.equals(LoginPage.user_username)) {
            buttonEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DiaryEntryActivity.this, AddNewLogActivity.class);
                    intent.putExtra("action", "edit");
                    intent.putExtra("log", log);
                    startActivityForResult(intent, 111);
                }
            });
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DiaryEntryActivity.this, PopupConfirmationActivity.class);
                    intent.putExtra("message", getResources().getString(R.string.log_delete_message));
                    intent.putExtra("yes", getResources().getString(R.string.log_delete_entry));
                    intent.putExtra("no", getResources().getString(R.string.log_cancel));
                    startActivityForResult(intent, 112);
                }
            });
        } else {
            // különben csak elrejti őket
            buttonEdit.setVisibility(View.GONE);
            buttonDelete.setVisibility(View.GONE);
        }


        loadLog();
    }

    private void loadLog() {
        String showId = intent.getStringExtra("showId");
        Context context = this;

        // az adatbázisból lekéri az adott id-jű előadást is, hogy a képet, stb. be tudja tölteni
        DatabaseReference mDatabase = Database.makeDatabase();
        mDatabase.child("shows").child(showId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        Models.Show show = snapshot.getValue(Models.Show.class);
                        show.setId(Integer.parseInt(snapshot.getKey()));

                        ImageView poster = findViewById(R.id.imageViewPoster);
                        Glide.with(context)
                                .load(show.getImgsrc())
                                .into(poster);

                        poster.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                BrowseShowsFragment.selectedShow = show;
                                finish();
                                Intent intent = new Intent(DiaryEntryActivity.this, ShowPageActivity.class);
                                startActivity(intent);
                            }
                        });

                        TextView title = findViewById(R.id.textViewTitle);
                        title.setText(show.getTitle());

                        TextView header = findViewById(R.id.textViewHeader);
                        if (!username.equals(LoginPage.user_username)) {
                            String headerText = username + " " + getResources().getString(R.string.log_saw);
                            header.setText(headerText);
                            header.setTypeface(null, Typeface.BOLD);

                            header.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(DiaryEntryActivity.this, UserProfile.class);
                                    intent.putExtra("username", username);
                                    startActivity(intent);
                                }
                            });
                        }

                        displayLogDataInInputFields();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("szabi", "onActivityResult");

        // 111 = szerkesztés
        if (requestCode == 111 && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("log")) {
                // az eredmény a szerkesztett bejegyzés objektum, amit átvesz,
                // és újra kiírja az adatait
                this.log = (Models.Log)data.getSerializableExtra("log");
                displayLogDataInInputFields();
            }
        } else if (requestCode == 112) { // 112 = törlés
            if (resultCode == RESULT_OK) {
                deleteThisEntry();
            } else {

            }
        }
    }

    // az adatbázisból törli a megnyitott bejegyzést, átlagot számol, ha volt értékelés,
    // és visszalép az előző oldalra
    private void deleteThisEntry() {
        DatabaseReference mDatabase = Database.makeDatabase();

        mDatabase.child("logs").child(username).child("count").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = snapshot.getValue(int.class);
                mDatabase.child("logs").child(username).child("count").setValue(count-1);

                if (log.getRating() > 0) {
                    String showId = log.getShow();
                    mDatabase.child("shows").child(showId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Models.Show show = snapshot.getValue(Models.Show.class);
                            show.setId(Integer.parseInt(snapshot.getKey()));

                            int ratingsCount = show.getRatings();
                            double ratingsAverage = show.getAverage();

                            ratingsCount -= 1;
                            mDatabase.child("shows").child(showId).child("ratings").setValue(ratingsCount);
                            ratingsAverage = (ratingsAverage * (ratingsCount+1) - log.getRating()) / ratingsCount;

                            ratingsAverage = Math.round(ratingsAverage * 100.0) / 100.0;

                            show.setRatings(ratingsCount);
                            show.setAverage((float)ratingsAverage);
                            BrowseShowsFragment.selectedShow = show;

                            mDatabase.child("shows").child(showId).child("average").setValue(ratingsAverage);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

                mDatabase.child("logs").child(username).child(log.getId()).removeValue();

                Toast.makeText(DiaryEntryActivity.this, getResources().getString(R.string.log_entry_deleted),
                        Toast.LENGTH_LONG).show();

                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DiaryEntryActivity.this, "error",
                        Toast.LENGTH_LONG).show();
            }
        });


    }

    private void displayLogDataInInputFields() {
        TextView date = findViewById(R.id.textViewDate2);
        TextView location = findViewById(R.id.textViewLocation2);
        TextView review = findViewById(R.id.textViewReview);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        CheckBox likedBox = findViewById(R.id.checkBoxLiked);

        date.setText(log.getDate());

        if (!log.getLocation().equals("")) {
            location.setText(log.getLocation());
        }

        if(!log.getReview().isEmpty()) {
            review.setText(log.getReview());
        }
        ratingBar.setRating((float) log.getRating());

        likedBox.setChecked(log.isLiked());
    }
}