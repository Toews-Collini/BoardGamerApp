package com.example.boardgamer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonParser;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;


public class FoodActivity extends AppCompatActivity {
    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    long spieltermin_id;
    int loading_tries;
    private final Gson gson = new Gson();
    BottomNavigationView bottomNavigation;
    RadioGroup rgFoodType;
    RadioButton rbGreek;
    RadioButton rbIndian;
    RadioButton rbItalian;
    RadioButton rbMexican;
    RadioButton rbTurkish;
    RadioButton rbGerman;
    Button btnSaveFoodChoice;
    ImageView btnBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_choice);

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

        rgFoodType = findViewById(R.id.foodTypeGroup);
        rbGerman = findViewById(R.id.germanOption);
        rbGerman.setChecked(true);
        rbItalian = findViewById(R.id.italianOption);
        rbGreek = findViewById(R.id.greekOption);
        rbIndian = findViewById(R.id.indianOption);
        rbMexican = findViewById(R.id.mexicanOption);
        rbTurkish = findViewById(R.id.turkishOption);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v-> finish());

        btnSaveFoodChoice = findViewById(R.id.saveFoodChoiceButton);
        btnSaveFoodChoice.setOnClickListener(v -> saveFoodChoice());

        loading_tries = 0;
        loadFoodChoice();
    }

    private void loadFoodChoice() {
        io.execute(() -> {
            try {
                String essenJson = supa.getFoodChoiceByIdAndPlayer(spieltermin_id, Spieler.appUserName);
                EssenWahl[] essenwahl = gson.fromJson(JsonParser.parseString(essenJson), EssenWahl[].class);
                if (essenwahl == null || essenwahl.length == 0) {
                    essenwahl = new EssenWahl[0];
                    com.google.gson.JsonObject row = new com.google.gson.JsonObject();
                    row.addProperty("spieltermin_id", spieltermin_id);
                    row.addProperty("spieler_name", Spieler.appUserName);
                    row.addProperty("essensrichtung_id", 1);

                    supa.insert("Gewaehlte_Essensrichtung",row);
                    loadFoodChoice();
                    runOnUiThread(() -> rbItalian.setChecked(true));
                    return;
                }

                Long choice = essenwahl[0].essensrichtung_id;
                final long wahl = (choice == null) ? 1 : choice;

                runOnUiThread(() -> {
                    if (wahl == 1) {
                        rbItalian.setChecked(true);
                    }
                    else if (wahl == 2) {
                        rbGreek.setChecked(true);
                    }
                    else if (wahl == 3) {
                        rbIndian.setChecked(true);
                    }
                    else if (wahl == 4) {
                        rbMexican.setChecked(true);
                    }
                    else if (wahl == 5) {
                        rbTurkish.setChecked(true);
                    }
                    else if (wahl == 6) {
                        rbGerman.setChecked(true);
                    }
                });
            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
                android.util.Log.e("loadFoodChoice", "Unerwarteter Fehler", e);
            }
        });
    }

    private void saveFoodChoice() {
        int wahl;

        if(rbItalian.isChecked()) {
            wahl = 1;
        }
        else if(rbGreek.isChecked()) {
            wahl = 2;
        }
        else if(rbIndian.isChecked()) {
            wahl = 3;
        }
        else if(rbMexican.isChecked()) {
            wahl = 4;
        }
        else if(rbTurkish.isChecked()) {
            wahl = 5;
        }
        else if(rbGerman.isChecked()) {
            wahl = 6;
        } else {
            wahl = 6;
        }

        io.execute(() -> {
            try {
                supa.updateGewaehlteEssensrichtung(spieltermin_id, Spieler.appUserName, wahl);
                main.post(() -> {
                    Toast.makeText(this, R.string.data_saved, Toast.LENGTH_SHORT).show();
                    loadFoodChoice();
                });
            } catch (Exception ex) {
                main.post(() -> Toast.makeText(this, R.string.insert_error, Toast.LENGTH_SHORT).show());
                android.util.Log.e("saveFoodChoice", "Update failed", ex);
            }
        });
    }
}