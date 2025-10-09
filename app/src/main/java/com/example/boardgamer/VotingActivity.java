package com.example.boardgamer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;

import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class VotingActivity extends AppCompatActivity {
    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    HashMap<Long, String> spiel_id2name = new HashMap<>();
    long spieltermin_id;
    private final Gson gson = new Gson();
    BottomNavigationView bottomNavigation;
    Button btnSaveVoting;
    ImageView ivBack;
    RecyclerView rvVoting;
    VotingAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting);
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

        btnSaveVoting = findViewById(R.id.saveVoting);
        ivBack = findViewById(R.id.btnBack);
        rvVoting = findViewById(R.id.votingRecyclerView);

        btnSaveVoting.setOnClickListener(v -> saveVoting());
        ivBack.setOnClickListener(v -> finish());

        adapter = new VotingAdapter();
        rvVoting.setLayoutManager(new LinearLayoutManager(this));
        rvVoting.setAdapter(adapter);

        loadVoting();
    }

    private void loadVoting() {
        io.execute(() -> {
            try {
                String votingJson = supa.getGespielteSpieleByIdAndPlayer(spieltermin_id, Spieler.appUserName);
                SpielVoting[] votings = gson.fromJson(votingJson, SpielVoting[].class);

                boolean hasVotes = votings != null && votings.length > 0;

                String spieleJson = supa.selectAll("Spiel");
                Spiel[] spiele = gson.fromJson(spieleJson, Spiel[].class);
                if (spiele == null || spiele.length == 0) return;

                spiel_id2name.clear();
                for (Spiel s : spiele) spiel_id2name.put(s.id, s.bezeichnung);

                java.util.LinkedHashMap<String,Integer> items = new java.util.LinkedHashMap<>();
                if (hasVotes) {
                    for (SpielVoting v : votings) {
                        String name = spiel_id2name.get(v.spiel_id);
                        if (name != null) items.put(name, v.spielerstimme_id);
                    }
                    main.post(() -> adapter.submitAll(items));
                }
                String vorschlaegeJson = supa.getVorgeschlageneSpieleById(spieltermin_id);
                Spielvorschlag[] vorschlaege = gson.fromJson(vorschlaegeJson, Spielvorschlag[].class);
                if (vorschlaege == null) vorschlaege = new Spielvorschlag[0];

                java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
                for (Spielvorschlag v : vorschlaege) {
                    if (v.spieltermin_id != spieltermin_id) continue;
                    String name = spiel_id2name.get(v.spiel_id);
                    if (name != null && !name.isEmpty()) names.add(name);
                }

                java.util.ArrayList<String> list = new java.util.ArrayList<>(names);
                java.util.Collections.sort(list, String::compareToIgnoreCase);

                main.post(() -> {
                    for (String n : list) {
                        boolean itemExists = false;
                        for (String item : items.keySet()) {
                            if (n.equals(item)) {
                                itemExists = true;
                            }
                        }
                        if (!itemExists) {
                            adapter.addItem(n);
                            HashMap<String, Long> name2id = new HashMap<>();
                            for (Map.Entry<Long, String> e : spiel_id2name.entrySet())
                                name2id.put(e.getValue(), e.getKey());
                            com.google.gson.JsonArray rows = new com.google.gson.JsonArray();
                            int state = 3;
                            Long spielId = name2id.get(n);
                            if (spielId == null) continue;

                            com.google.gson.JsonObject row = new com.google.gson.JsonObject();
                            row.addProperty("spieltermin_id", spieltermin_id);
                            row.addProperty("spiel_id", spielId);
                            row.addProperty("spieler_name", Spieler.appUserName);
                            row.addProperty("spielerstimme_id", state);
                            rows.add(row);

                            io.execute(() -> {
                                try {
                                    supa.upsertMany(
                                            "Gespielte_Spiele",
                                            rows,
                                            "spieltermin_id,spieler_name,spiel_id"
                                    );
                                } catch (Exception ex) {
                                    main.post(() -> Toast.makeText(this, "Fehler: " + ex.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
                                    android.util.Log.e("loadVoting", "Unerwarteter Fehler", ex);
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show());
                android.util.Log.e("loadVoting", "Unerwarteter Fehler", e);
            }
        });
    }


    private void saveVoting() {
        Map<String, Integer> votes = adapter.snapshot();
        if (votes.isEmpty()) { Toast.makeText(this,"Keine Änderungen",Toast.LENGTH_SHORT).show(); return; }

        HashMap<String, Long> name2id = new HashMap<>();
        for (Map.Entry<Long, String> e : spiel_id2name.entrySet()) name2id.put(e.getValue(), e.getKey());

        java.util.List<Map.Entry<String,Integer>> toUpdate = new java.util.ArrayList<>();
        for (Map.Entry<String,Integer> e : votes.entrySet()) {
            Integer v = e.getValue();
            if (v != null && v > 0) toUpdate.add(e);
        }
        if (toUpdate.isEmpty()) { return; }

        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(toUpdate.size());

        for (Map.Entry<String,Integer> e : toUpdate) {
            Long spielId = name2id.get(e.getKey());
            if (spielId == null) { if (remaining.decrementAndGet()==0) main.post(this::loadVoting); continue; }

            int state = e.getValue();
            io.execute(() -> {
                try {
                    supa.updateGespielteSpiele(spieltermin_id, spielId, Spieler.appUserName, state);
                } catch (Exception ex) {
                    main.post(() -> Toast.makeText(this, R.string.insert_error, Toast.LENGTH_SHORT).show());
                    android.util.Log.e("saveVoting", "Update failed", ex);
                } finally {
                    if (remaining.decrementAndGet() == 0) {
                        main.post(() -> {
                            Toast.makeText(this, R.string.data_saved, Toast.LENGTH_SHORT).show();
                            loadVoting();
                        });
                    }
                }
            });
        }
    }
}