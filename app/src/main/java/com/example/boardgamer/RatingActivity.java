package com.example.boardgamer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonParser;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;

public class RatingActivity extends AppCompatActivity {
    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    long spieltermin_id;
    private final Gson gson = new Gson();
    BottomNavigationView bottomNavigation;
    Button btnSaveRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        supa = new SupabaseClient(this);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id == R.id.home) {
                Intent i = new Intent(this, HomeActivity.class);
                startActivity(i);
            }
            if(id == R.id.messages) {
                Intent i = new Intent(this, MessagesActivity.class);
                startActivity(i);
            }
            else if(id == R.id.settings) {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }
            return true;
        });

        Intent intent = getIntent();
        spieltermin_id = intent.getLongExtra("spieltermin_id", -1);

        btnSaveRating = findViewById(R.id.ratingButton);

    //    btnSaveRating.setOnClickListener(v -> saveRatings());

    //    loadVoting();
    }
/*
    private void loadVoting() {
        io.execute(() -> {
            try {
                String ratingJson = supa.getRatingByIdAndPlayer(spieltermin_id, Spieler.appUserName);
                SpieleabendRating[] all = gson.fromJson(JsonParser.parseString(ratingJson), SpieleabendRating[].class);
                if (all == null || all.length == 0) {
                    all = new SpieleabendRating[0];
                    return;
                }
            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
                android.util.Log.e("loadVoting", "Unerwarteter Fehler", e);
            }
        });
    }

 */
}