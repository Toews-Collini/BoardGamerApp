package com.example.boardgamer;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
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
    RatingBar rbHost;
    EditText etHost;
    RatingBar rbFood;
    EditText etFood;
    RatingBar rbEvening;
    EditText etEvening;
    Button btnSaveRating;
    int loading_tries = 0;

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

        rbHost = findViewById(R.id.ratingBarHost);
        etHost = findViewById(R.id.tvRatingHost);
        rbFood = findViewById(R.id.ratingBarFood);
        etFood = findViewById(R.id.tvRatingFood);
        rbEvening = findViewById(R.id.ratingBarEvening);
        etEvening = findViewById(R.id.tvRatingEvening);

        btnSaveRating = findViewById(R.id.ratingButton);
        btnSaveRating.setOnClickListener(v -> saveRatings());

        loadRatings();
    }

    private void loadRatings() {
        io.execute(() -> {
            try {
                String ratingJson = supa.getRatingByIdAndPlayer(spieltermin_id, Spieler.appUserName);
                SpieleabendRating[] spieleabend = gson.fromJson(JsonParser.parseString(ratingJson), SpieleabendRating[].class);
                if (spieleabend == null || spieleabend.length == 0) {
                    spieleabend = new SpieleabendRating[0];
                    com.google.gson.JsonArray rows = new com.google.gson.JsonArray();
                    com.google.gson.JsonObject row = new com.google.gson.JsonObject();
                    row.addProperty("spieltermin_id", spieltermin_id);
                    row.addProperty("spieler_name", Spieler.appUserName);
                    rows.add(row);

                    supa.upsertMany(
                            "Spieleabend",
                            rows,
                            "spieltermin_id,spieler_name"
                    );

                    if(loading_tries == 0) {
                        loading_tries++;
                        loadRatings();
                    }

                    Toast.makeText(this, R.string.loading_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                int host_stars = spieleabend[0].gastgeber_sterne == null ? 0 : spieleabend[0].gastgeber_sterne;
                String host_comment = spieleabend[0].gastgeber_kommentar == null ? "" : spieleabend[0].gastgeber_kommentar;
                int food_stars = spieleabend[0].essen_sterne == null ? 0 : spieleabend[0].essen_sterne;
                String food_comment = spieleabend[0].essen_kommentar == null ? "" : spieleabend[0].essen_kommentar;
                int evening_stars = spieleabend[0].abend_sterne == null ? 0 : spieleabend[0].abend_sterne;
                String evening_comment = spieleabend[0].abend_kommentar == null ? "" : spieleabend[0].abend_kommentar;

                runOnUiThread(() -> {
                    rbHost.setRating(host_stars);
                    etHost.setText(host_comment);
                    rbFood.setRating(food_stars);
                    etFood.setText(food_comment);
                    rbEvening.setRating(evening_stars);
                    etEvening.setText(evening_comment);
                });

            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
                android.util.Log.e("loadRatings", "Unerwarteter Fehler", e);
            }
        });
    }

    private void saveRatings() {
        io.execute(() -> {
            try {
                supa.updateSpieleabend(spieltermin_id, Spieler.appUserName, (int)rbHost.getRating(), etHost.getText().toString().trim(),
                        (int)rbFood.getRating(), etFood.getText().toString().trim(), (int) rbEvening.getRating(), etEvening.getText().toString().trim());
                main.post(() -> {
                    Toast.makeText(this, R.string.data_saved, Toast.LENGTH_SHORT).show();
                    loadRatings();
                });
            } catch (Exception ex) {
                main.post(() -> Toast.makeText(this, R.string.insert_error, Toast.LENGTH_SHORT).show());
                android.util.Log.e("saveRatings", "Update failed", ex);
            }
        });
    }
}