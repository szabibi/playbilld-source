package com.example.playbilld;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// ezzel az osztállyal könnyebb létrehozni egy adatbázis referenciát
public class Database {

    public static DatabaseReference makeDatabase() {
        DatabaseReference db = FirebaseDatabase
                .getInstance("https://playbilld-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference();

        return db;
    }

}
