package com.example.boardgamer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class SuggestionsActivity extends AppCompatActivity {
    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    Spiel[] spielArray;
    long spieltermin_id;
    private final Gson gson = new Gson();
    BottomNavigationView bottomNavigation;
    EditText etSuggestion;
    Button btnAddSuggestion;
    RecyclerView rvSuggestions;
    SuggestionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);
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

        etSuggestion = findViewById(R.id.suggestionInput);
        btnAddSuggestion = findViewById(R.id.btnAddSuggestion);
        rvSuggestions = findViewById(R.id.suggestionsList);

        btnAddSuggestion.setOnClickListener(v -> addSuggestion(etSuggestion.getText().toString().trim()));

        adapter = new SuggestionAdapter(new ArrayList<>());
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(adapter);

        loadSuggestions();
    }

    private void addSuggestion(String suggestion) {
        if (suggestion.isEmpty()) return;
        io.execute(() -> {
            try {
                long spielId = -1L;
                if (spielArray != null) {
                    for (Spiel s : spielArray) {
                        if (suggestion.equals(s.bezeichnung)) { spielId = s.id; break; }
                    }
                }
                if (spielId == -1L) {
                    JsonObject spiel = new JsonObject();
                    spiel.addProperty("bezeichnung", suggestion);
                    String resp = supa.insert("Spiel", spiel);
                    org.json.JSONArray arr = new org.json.JSONArray(resp);
                    spielId = arr.getJSONObject(0).getLong("id");
                }

                JsonObject sv = new JsonObject();
                sv.addProperty("spieltermin_id", spieltermin_id);
                sv.addProperty("spiel_id", spielId);
                supa.insert("Vorgeschlagene_Spiele", sv);

                renderSuggestion(suggestion);

            } catch (Exception e) {
                Toast.makeText(this, R.string.insert_error, Toast.LENGTH_SHORT).show();
                android.util.Log.e("addSuggestionFromUser", "Fehler", e);
            }
        });
    }

    private void loadSuggestions() {
        io.execute(() -> {
            try {
                String spielvorschlagJson = supa.getVorgeschlageneSpieleById(spieltermin_id);
                Spielvorschlag[] vorschlaege = gson.fromJson(
                        JsonParser.parseString(spielvorschlagJson), Spielvorschlag[].class);
                if (vorschlaege == null) vorschlaege = new Spielvorschlag[0];

                String spielJson = supa.selectAll("Spiel");
                spielArray = gson.fromJson(JsonParser.parseString(spielJson), Spiel[].class);
                if (spielArray == null) spielArray = new Spiel[0];

                java.util.HashMap<Long,String> id2name = new java.util.HashMap<>();
                for (Spiel s : spielArray) id2name.put(s.id, s.bezeichnung);
                java.util.List<String> texts = new java.util.ArrayList<>();
                for (Spielvorschlag v : vorschlaege) {
                    String name = id2name.get(v.spiel_id);
                    if (name != null) texts.add(name);
                }

                main.post(() -> {
                    for (String t : texts) {
                        int pos = adapter.addSuggestion(t);
                    }
                });

            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(),
                        Toast.LENGTH_LONG).show());
                android.util.Log.e("loadSuggestions", "Unerwarteter Fehler", e);
            }
        });
    }

    private void renderSuggestion(String text) {
        if (text == null || text.trim().isEmpty()) return;
        main.post(() -> {
            int pos = adapter.addSuggestion(text.trim());
            rvSuggestions.scrollToPosition(pos);
        });
    }
}