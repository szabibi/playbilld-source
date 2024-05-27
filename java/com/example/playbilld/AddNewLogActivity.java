package com.example.playbilld;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AddNewLogActivity extends AppCompatActivity {

    // dátumválasztás
    private DatePickerDialog datePickerDialog;

    private boolean hasSelectedDate = false;

    // az előadás amihez készül a bejegyzés
    private Models.Show show;

    // a bejegyzés adatainak bemeneti mezői, gombjai
    private Button buttonDate;
    private Button buttonLocation;
    private String username;
    private String showId;
    private RatingBar ratingBar;
    private CheckBox likedBox;
    private EditText reviewField;

    // amennyiben szerkesztés zajlik, ezeket is ismerni kell
    private Models.Log log;
    private float originalRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_log);

        // dátumválasztás gomb beállítása
        buttonDate = findViewById(R.id.buttonDate);
        buttonDate.setText(getCurrentDate());
        initDatePicker();

        buttonDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });

        // helyszínválasztó gomb
        buttonLocation = findViewById(R.id.buttonLocation);
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(AddNewLogActivity.this, MapsActivityCurrentPlace.class);
                startActivityForResult(intent, 1000);
            }
        });

        // bemeneti mezők inicializálása
        username = LoginPage.user_username;
        ratingBar = findViewById(R.id.ratingBar);
        likedBox = findViewById(R.id.checkBoxLiked);
        reviewField = findViewById(R.id.editTextTextMultiLineReview);

        // itt határozza meg, hogy szerkesztés zajlik-e
        // ha igen, a szerkesztendő bejegyzést is átveszi
        Intent intent = getIntent();
        if (intent.getStringExtra("action").equals("edit")) {
            Log.d("szabi", "editing log");
            setupPageForEditingLog();
        } else {
            // különben létrehozás zajlik
            setupPageForCreatingLog();
        }
    }

    private void displayShowData(String imgSrc, String title) {
        // a felületet feltölti az előadás képével, címével

        ImageView poster = findViewById(R.id.imageViewPoster);
        Glide.with(this)
                .load(imgSrc)
                .into(poster);

        TextView titleView = findViewById(R.id.textViewTitle);
        titleView.setText(title);
    }

    private void setupPageForCreatingLog() {
        // ha létrehozás zajlik, akkor beállítja magának, hogy mely előadás oldalát néztük
        // ez az előadás a BrowseShowsFragment statikus tulajdonsága, de más activity
        // is átállítja néha
        show = BrowseShowsFragment.selectedShow;
        showId = Integer.toString(show.getId());

        displayShowData(show.getImgsrc(), show.getTitle());

        // és a "Kész" gombhoz a létrehozás funkciót adja hozzá
        Button buttonAdd = findViewById(R.id.buttonCreateLog);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitLog();
            }
        });
    }

    private void setupPageForEditingLog() {
        Intent intent = getIntent();
        log = (Models.Log) intent.getSerializableExtra("log");
        showId = log.getShow();

        displayShowData(log.getShowImgSrc(), log.getShowTitle());

        // lekéri a bejegyzéshez tartozó értékeket (értékelés, vélemény, stb...)
        // és a bemeneti mezőket feltölti velük
        originalRating = (float) log.getRating();
        ratingBar.setRating(originalRating);

        likedBox.setChecked(log.isLiked());
        reviewField.setText(log.getReview());
        buttonDate.setText(log.getDate());

        String location = log.getLocation();
        if (!location.isEmpty()) {
            buttonLocation.setText(location);
        }

        // és a "Kész" gombhoz a szerkesztés funkciót adja hozzá
        Button buttonAdd = findViewById(R.id.buttonCreateLog);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editLog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1000 = térkép activity
        if(requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            String locationName = data.getStringExtra("locationName");
            if (!locationName.isEmpty()) {
                buttonLocation.setText(locationName);
            } else {
                buttonLocation.setText(getResources().getString(R.string.log_select_location).toString());
            }
        }
    }

    private void submitLog() {
        // bemeneti mezők értékeinek lekérése
        float rating = ratingBar.getRating();

        String date = buttonDate.getText().toString();

        boolean liked = likedBox.isChecked();

        String review = reviewField.getText().toString();

        // adatbázishoz hozzáadás
        DatabaseReference mDatabase = Database.makeDatabase();

        mDatabase.child("logs").child(username).child("count")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // bejegyzésének számának lekérése
                        int count;
                        if (dataSnapshot.exists()) {
                            count = dataSnapshot.getValue(Integer.class);
                        } else {
                            count = 0;
                        }

                        // bejegyzések számának növelése
                        DatabaseReference newLogReference = mDatabase.child("logs")
                                .child(username)
                                .child(Integer.toString(count+1));

                        // bejegyzés feltöltése adatbázisba
                        newLogReference.child("date").setValue(date);
                        newLogReference.child("liked").setValue(liked);

                        if (buttonLocation.getText().toString().equals("Select location...")) {
                            newLogReference.child("location").setValue("");
                        } else {
                            newLogReference.child("location").setValue(buttonLocation.getText());
                        }
                        newLogReference.child("rating").setValue(rating);
                        newLogReference.child("review").setValue(review);
                        newLogReference.child("show").setValue(showId);
                        newLogReference.child("showImgSrc").setValue(show.getImgsrc());
                        newLogReference.child("showTitle").setValue(show.getTitle());

                        // bejegyzések számának növelése
                        mDatabase.child("logs")
                                .child(username)
                                .child("count").setValue(count+1);

                        // átlagszámítás
                        if (rating > 0) {

                            int ratingsCount = show.getRatings();
                            double ratingsAverage = show.getAverage();

                            ratingsAverage = (ratingsAverage * ratingsCount + rating) / (ratingsCount + 1);
                            ratingsAverage = Math.round(ratingsAverage*100.0)/100.0;

                            // a megtekintett előadás objektum adatait is megváltoztatja (selectedShow)
                            BrowseShowsFragment.selectedShow.setAverage((float)ratingsAverage);
                            BrowseShowsFragment.selectedShow.incrementRatings();
                            mDatabase.child("shows").child(showId).child("ratings").setValue(ratingsCount+1);
                            mDatabase.child("shows").child(showId).child("average").setValue(ratingsAverage);

                            Toast.makeText(AddNewLogActivity.this, getResources().getString(R.string.log_entry_added),
                                    Toast.LENGTH_LONG).show();

                        }

                        finish();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void editLog() {
        // ez a metódus mind az adatbázisban, mind a megtekintett bejegyzsében
        // megváltoztatja az értékeket az újra

        float rating = ratingBar.getRating();
        String date = buttonDate.getText().toString();
        boolean liked = likedBox.isChecked();
        String review = reviewField.getText().toString();

        String logId = log.getId();

        DatabaseReference mDatabase = Database.makeDatabase();

        DatabaseReference logReference = mDatabase.child("logs")
                .child(username)
                .child(logId);

        logReference.child("date").setValue(date);
        log.setDate(date);
        logReference.child("liked").setValue(liked);
        log.setLiked(liked);

        String location = buttonLocation.getText().toString();
        if (location.equals("Select location...")) {
            logReference.child("location").setValue("");
            log.setLocation("");
        } else {
            logReference.child("location").setValue(location);
            log.setLocation(location);
        }
        Log.d("szabi", "log location after edit: " + log.getLocation());

        logReference.child("rating").setValue(rating);
        log.setRating(rating);
        logReference.child("review").setValue(review);
        log.setReview(review);

        Log.d("szabi", log.toString());

        // az átlagszámításhoz lekéri az adatbázisból az adott előadást
        mDatabase.child("shows").child(showId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        Models.Show show = snapshot.getValue(Models.Show.class);

                        // hogy az ide-oda lépegetés során ne legyen ebből baj,
                        // a megtekintett előadást is beállítja erre
                        BrowseShowsFragment.selectedShow = show;

                        int ratingsCount = show.getRatings();
                        double ratingsAverage = show.getAverage();

                        // van megadva csillagos értékelés...
                        if (rating > 0) {
                            // ...de eretileg nem volt
                            // --> növelni kell az előadás értékeléseinek számát
                            if (originalRating == 0) {
                                BrowseShowsFragment.selectedShow.incrementRatings();
                                ratingsCount += 1;
                                mDatabase.child("shows").child(showId).child("ratings").setValue(ratingsCount);
                                ratingsAverage = (ratingsAverage * (ratingsCount-1) - originalRating + rating) / ratingsCount;
                            } else {
                                // volt eredetileg is értékelés, csak kiszámolja az új átlagot
                                ratingsAverage = (ratingsAverage * (ratingsCount) - originalRating + rating) / ratingsCount;
                            }

                            ratingsAverage = Math.round(ratingsAverage * 100.0) / 100.0;

                            BrowseShowsFragment.selectedShow.setAverage((float) ratingsAverage);

                            mDatabase.child("shows").child(showId).child("average").setValue(ratingsAverage);
                        } else {
                            // most már nincs értékelés...
                            if (originalRating > 0) {
                                // ....de eredetileg volt --> csökkentjük az előadás értékeléseinek számát
                                BrowseShowsFragment.selectedShow.decrementRatings();
                                ratingsCount -= 1;
                                mDatabase.child("shows").child(showId).child("ratings").setValue(ratingsCount);
                                ratingsAverage = (ratingsAverage * (ratingsCount+1) - originalRating) / ratingsCount;

                                ratingsAverage = Math.round(ratingsAverage * 100.0) / 100.0;

                                BrowseShowsFragment.selectedShow.setAverage((float) ratingsAverage);

                                mDatabase.child("shows").child(showId).child("average").setValue(ratingsAverage);

                            }
                        }

                        Toast.makeText(AddNewLogActivity.this, getResources().getString(R.string.log_entry_updated),
                                Toast.LENGTH_LONG).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("log", log);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return makeDateString(year, month, day);
    }

    private void initDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month + 1;
                String date = makeDateString(year, month, dayOfMonth);
                buttonDate.setText(date);
            }
        };

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int style = AlertDialog.THEME_HOLO_LIGHT;

        datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);
    }

    private String makeDateString(int year, int month, int dayOfMonth) {
        String date = year + "-";
        if (month < 10) {
            date += "0";
        }
        date += month + "-";
        if (dayOfMonth < 10) {
            date += "0";
        }
        date += dayOfMonth;
        return date;
    }

    public void cancelLog(View view) {
       finish();
    }
}