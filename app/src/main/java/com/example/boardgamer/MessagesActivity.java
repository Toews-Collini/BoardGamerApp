package com.example.boardgamer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;

public class MessagesActivity extends AppCompatActivity {
    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    Spiel[] spielArray;
    long spieltermin_id;
    private final Gson gson = new Gson();
    BottomNavigationView bottomNavigation;
    EditText etChatInput;
    Button btnSendMessage;
    RecyclerView rvChatMessage;
    MessageAdapter adapter;
    Boolean isOwn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        supa = new SupabaseClient(this);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id == R.id.home) {
                Intent i = new Intent(this, HomeActivity.class);
                startActivity(i);
            }
/*
            if(id == R.id.messages) {
                Intent i = new Intent(this, MessagesActivity.class);
                startActivity(i);
            }

 */
            else if(id == R.id.settings) {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }
            return true;
        });

        Intent intent = getIntent();
        spieltermin_id = intent.getLongExtra("spieltermin_id", -1);

        etChatInput = findViewById(R.id.chatInput);
        btnSendMessage = findViewById(R.id.sendMessage);
        rvChatMessage = findViewById(R.id.ChatMessagesRecyclerView);

        btnSendMessage.setOnClickListener(v -> addMessage(isOwn, etChatInput.getText().toString().trim()));

        adapter = new MessageAdapter(new ArrayList<>());
        rvChatMessage.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessage.setAdapter(adapter);

        loadMessages();
    }

    private void addMessage(Boolean isOwn, String message) {
        io.execute(() -> {
            try {
                JsonObject nachricht = new JsonObject();
                nachricht.addProperty("spieler_name", Spieler.appUserName);
                nachricht.addProperty("nachricht", message);
                supa.insert("Nachricht", nachricht);

                renderSuggestion(true, Spieler.appUserName + ":\n" + message);

            } catch (Exception e) {
                Toast.makeText(this, R.string.insert_error, Toast.LENGTH_SHORT).show();
                android.util.Log.e("addMessageFromUser", "Fehler", e);
            }
        });
    }

    private void loadMessages() {
        io.execute(() -> {
            try {
                String nachrichtJson = supa.selectAllOrderById("Nachricht");
                Nachricht[] nachrichten = gson.fromJson(
                        JsonParser.parseString(nachrichtJson), Nachricht[].class);
                if (nachrichten == null) return;

                main.post(() -> {
                    for (Nachricht n : nachrichten) {
                        boolean isOwn = n.spieler_name.equals(Spieler.appUserName);
                        int pos = adapter.addItem(isOwn, n.spieler_name + ":\n" + n.nachricht);
                    }
                });

            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, R.string.loading_failed, Toast.LENGTH_LONG).show());
                android.util.Log.e("loadMessages", "Unerwarteter Fehler", e);
            }
        });
    }

    private void renderSuggestion(boolean isOwn, String text) {
        main.post(() -> {
            int pos = adapter.addItem(isOwn, text.trim());
            rvChatMessage.scrollToPosition(pos);
        });
    }
}