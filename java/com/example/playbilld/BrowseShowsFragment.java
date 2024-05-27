package com.example.playbilld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrowseShowsFragment extends Fragment {

    // adatbázis
    private DatabaseReference mDatabase;

    // kereséshez szükséges
    private EditText searchBar;
    private String searchKey;
    private List<Models.Show> showsFullList;

    // megtekintett/clicked előadás
    public static Models.Show selectedShow;

    View rootView;
    Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_browse_shows, container, false);
        activity = getActivity();

        searchBar = rootView.findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {            }

            @Override
            public void afterTextChanged(Editable s) {
                searchKey = searchBar.getText().toString();
                displaySearchResults();
            }
        });

        mDatabase = Database.makeDatabase();

        // összes előadás betöltése adatbázisból, később csak ebben keres
        showsFullList = new ArrayList<>();
        mDatabase.child("shows")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Models.Show show = snapshot.getValue(Models.Show.class);
                            show.setId(Integer.parseInt(snapshot.getKey()));
                            Log.d("szabi", show.toString());
                            showsFullList.add(show);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        searchBar.setText(searchKey);
    }

    private void displaySearchResults() {
        final Context context = activity;
        final LinearLayout main = rootView.findViewById(R.id.searchResultLayout);
        main.removeAllViews();
        final String searchKey = searchBar.getText().toString().toLowerCase();

        List<Models.Show> matchAtStart = new ArrayList<>();
        List<Models.Show> matchWithin = new ArrayList<>();
        List<Models.Show> searchResults = new ArrayList<>();

        for (Models.Show show : showsFullList) {
            Log.d("szabi", show.toString());
            int matchIdx = show.getTitle().toLowerCase().indexOf(searchKey);
            if (matchIdx == 0) {
                matchAtStart.add(show);
            } else if (matchIdx != -1) {
                matchWithin.add(show);
            }
        }

        Collections.sort(matchAtStart);
        Collections.sort(matchWithin);

        searchResults.addAll(matchAtStart);
        searchResults.addAll(matchWithin);

        // keresési eredmény dobozok létrehozása
        for (Models.Show show : searchResults) {
            ViewStub stub = new ViewStub(context);
            stub.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT));
            main.addView(stub);
            stub.setLayoutResource(R.layout.search_result);
            View inflated_view = stub.inflate();

            // sa dobozok tag-je az előadás azonosítója, így rájuk kattintáskor
            // ez használható
            inflated_view.setTag(show);

            // háttérszín kezelése és kattintáskor a megtekintett előadás beállítása
            // és az oldalának megnyitása
            inflated_view.setOnTouchListener(new View.OnTouchListener() {
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
                        selectedShow = (Models.Show)v.getTag();
                        Intent intent = new Intent(requireContext(), ShowPageActivity.class);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            });

            // kép, cím és ősbemutató évének beállítása a dobozon belül
            ImageView image = inflated_view.findViewById(R.id.imageViewPoster);
            Glide.with(context)
                    .load(show.getImgsrc())
                    .into(image);
            TextView title = inflated_view.findViewById(R.id.textViewTitle);
            title.setText(show.getTitle());

            TextView premiere = inflated_view.findViewById(R.id.textViewPremiereDate);
            premiere.setText(show.getPremiereString());
        }
    }

}