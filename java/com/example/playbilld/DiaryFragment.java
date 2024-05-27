package com.example.playbilld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// összes saját létrehozott bejegyzést listázó fragment
public class DiaryFragment extends Fragment {
    private List<Models.Log> logs = new ArrayList<>();

    // LoginPage-ből átvett username
    private String username;

    // kijelölt bejegyzés és hozzátartozó előadás azonosítója
    public static String selectedLogShowId;
    public static Models.Log selectedLog;

    View rootView;
    Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_diary, container, false);
        activity = getActivity();

        username = LoginPage.user_username;

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadAllLogs();
    }

    private void loadAllLogs() {
        Context context = requireContext();
        Fragment me = this;
        final LinearLayout main = rootView.findViewById(R.id.logsLayout);
        main.removeAllViews();

        logs.clear();

        DatabaseReference mDatabase = Database.makeDatabase();

        Log.d("userlogs", mDatabase.child("logs").child(username).toString());
        mDatabase.child("logs").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // bejegyzések kigyűjtése adatbázisból
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.getKey().toString().equals("count") == false) {
                                Models.Log log = snapshot.getValue(Models.Log.class).buildRest(username);
                                log.setId(snapshot.getKey());
                                logs.add(log);
                            }
                        }

                        // dátum szerinti rendezés és megfordítás
                        Collections.sort(logs);
                        Collections.reverse(logs);

                        // ez az objektum egy hónapot és egy évszámot, hogy
                        // számon lehessen tartani, éppen mikor bejegyzés lett kirajzolva
                        MonthYear monthYear = new MonthYear();
                        monthYear.setMonth(-1);

                        for (Models.Log log : logs) {

                            // ha megváltozik a hónap vagy év, akkor létrehoz egy
                            // ViewStub-ot, ami egy évszám-hónap szöveget ír ki
                            if (log.getMonth() != monthYear.getMonth() || log.getYear() != monthYear.getYear()) {
                                monthYear.setMonth(log.getMonth());
                                monthYear.setYear(log.getYear());


                                ViewStub stub = new ViewStub(context);
                                stub.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT));
                                main.addView(stub);
                                stub.setLayoutResource(R.layout.log_month_header);
                                View inflated_view = stub.inflate();

                                TextView date = inflated_view.findViewById(R.id.textViewDate);
                                String date_full = MonthYear.getMonthName(me, monthYear.getMonth()) + " " + Integer.toString(monthYear.getYear());
                                date.setText(date_full);
                            }

                            // bejegyzés ViewStubjának létrehozása, és a napszám, kép és címmel feltöltés
                            ViewStub log_stub = new ViewStub(context);
                            log_stub.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT));
                            main.addView(log_stub);
                            log_stub.setLayoutResource(R.layout.log);
                            View log_inflated_view = log_stub.inflate();

                            // a tag maga a log objektum, hogy rákattintva fel lehessen használni
                            log_inflated_view.setTag(log);

                            TextView day = log_inflated_view.findViewById(R.id.textViewDay);
                            day.setText(Integer.toString(log.getDay()));

                            ImageView poster = log_inflated_view.findViewById(R.id.imageViewPoster);

                            Glide.with(context)
                                    .load(log.getShowImgSrc())
                                    .into(poster);

                            TextView title = log_inflated_view.findViewById(R.id.textViewTitle);
                            title.setText(log.getShowTitle());

                            log_inflated_view.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                        v.setBackgroundColor(getResources().getColor(R.color.light_gray));
                                        return true;
                                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                                        v.setBackgroundColor(getResources().getColor(R.color.transparent));
                                        return true;
                                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                                        v.setBackgroundColor(getResources().getColor(R.color.transparent));
                                        selectedLogShowId = log.getShow();
                                        selectedLog = (Models.Log)v.getTag();
                                        // bejegyzés részletek oldalának megnyitása
                                        Intent intent = new Intent(requireContext(), DiaryEntryActivity.class);
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
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("databaseerror", "Failed to get data");
                    }
                });
    }
}