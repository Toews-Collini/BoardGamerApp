package com.example.boardgamer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;


import java.util.ArrayList;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;

public class FoodChoiceOverview extends AppCompatActivity {
    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    Essensrichtung[] essensrichtungArray;
    long spieltermin_id;
    private final Gson gson = new Gson();
    BottomNavigationView bottomNavigation;
    RecyclerView rvFoodChoices;
    FoodChoiceAdapter adapter;
    TextView tvMajorityFoodChoice;

    private class CardItem {
        String guestname;
        String foodChoice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_choice_overview);

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

        rvFoodChoices = findViewById(R.id.foodChoiceRecyclerView);
        tvMajorityFoodChoice = findViewById(R.id.majorityFoodChoice);

        adapter = new FoodChoiceAdapter(new ArrayList<>());
        rvFoodChoices.setLayoutManager(new LinearLayoutManager(this));
        rvFoodChoices.setAdapter(adapter);

        loadFoodChoices();
    }

    private void addFoodChoice(String guestName, String foodChoice) {
        if (guestName.isEmpty()) return;
        io.execute(() -> {
            try {
                long essensrichtungId = -1L;
                if (essensrichtungArray != null) {
                    for (Essensrichtung e : essensrichtungArray) {
                        if (foodChoice.equals(e.bezeichnung)) { essensrichtungId = e.id; break; }
                    }
                }

                renderSuggestion(guestName, foodChoice);

            } catch (Exception e) {
                Toast.makeText(this, R.string.insert_error, Toast.LENGTH_SHORT).show();
                android.util.Log.e("addSuggestionFromUser", "Fehler", e);
            }
        });
    }

    private void loadFoodChoices() {
        io.execute(() -> {
            try {
                String essenwahlJson = supa.getGewaehlteEssenrichtungById(spieltermin_id);
                EssenWahl[] wahl = gson.fromJson(
                        JsonParser.parseString(essenwahlJson), EssenWahl[].class);
                if (wahl == null) wahl = new EssenWahl[0];

                String essensrichtungJson = supa.selectAll("Essensrichtung");
                essensrichtungArray = gson.fromJson(JsonParser.parseString(essensrichtungJson), Essensrichtung[].class);
                if (essensrichtungArray == null) {
                    essensrichtungArray = new Essensrichtung[0];
                }

                java.util.HashMap<Long, String> id2name = new java.util.HashMap<>();
                for (Essensrichtung s : essensrichtungArray)
                    id2name.put(s.id, s.bezeichnung);

                ArrayList<Integer> foodChoices = new ArrayList<>(essensrichtungArray.length);
                for (int i = 0; i < essensrichtungArray.length; i++) {
                    foodChoices.add(0);
                }

                java.util.List<CardItem> items = new java.util.ArrayList<>();

                for (EssenWahl w : wahl) {
                    FoodChoiceOverview.CardItem cardItem = new CardItem();
                    String foodName = id2name.get(w.essensrichtung_id);

                    int foodChoicesIndex = (int) w.essensrichtung_id - 1;
                    foodChoices.set(foodChoicesIndex, foodChoices.get(foodChoicesIndex) + 1);

                    cardItem.guestname = w.spieler_name;
                    cardItem.foodChoice = foodName;
                    items.add(cardItem);
                }

                int max = Collections.max(foodChoices);

                List<Integer> winners = new ArrayList<>();
                for (int i = 0; i < foodChoices.size(); i++) {
                    if (foodChoices.get(i) == max) {
                        winners.add(i);
                    }
                }

                String text;
                if (winners.size() == 1) {
                    int winnerIndex = winners.get(0);
                    long id = winnerIndex + 1L;
                    String name = id2name.get(id);
                    text = getString(R.string.majorityFoodChoice) + "\n" + name;
                } else {
                    text = getString(R.string.majorityFoodChoice) + "\n" + getString(R.string.drawn);
                }

                String finalText = text;
                main.post(() -> {
                    for (CardItem i : items) {
                        int pos = adapter.addFoodChoice("Guest name: " + i.guestname, "Food choice: " + i.foodChoice);
                    }
                    tvMajorityFoodChoice.setText(finalText);
                });

            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(),
                        Toast.LENGTH_LONG).show());
                android.util.Log.e("loadFoodChoices", "Unerwarteter Fehler", e);
            }
        });
    }

    private void renderSuggestion(String guestName, String foodChoice) {
        if (guestName == null || guestName.trim().isEmpty()) return;
        main.post(() -> {
            int pos = adapter.addFoodChoice(guestName.trim(), foodChoice);
            rvFoodChoices.scrollToPosition(pos);
        });
    }
}