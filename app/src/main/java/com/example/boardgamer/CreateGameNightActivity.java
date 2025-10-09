package com.example.boardgamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class CreateGameNightActivity extends AppCompatActivity {
    private static final String TAG = "SpielerJson";

    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    SupabaseClient supa;
    private final Gson gson = new Gson();
    EditText gameNightDate;
    String gameNightTime = "00:00";
    Button btnEditTime;
    Button btnInsertGameNightData;

    private DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game_night);

        gameNightDate = findViewById(R.id.editGameNightDate);
        btnInsertGameNightData = findViewById(R.id.insertGameNightDataButton);
        btnEditTime = findViewById(R.id.editTimeButton);

        btnEditTime.setOnClickListener(view -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H) // << hier 24h aktivieren
                    .setHour(14)
                    .setMinute(30)
                    .setTitleText("Zeit auswählen")
                    .build();

            picker.show(getSupportFragmentManager(), "TIME_PICKER");

            picker.addOnPositiveButtonClickListener(v -> {
                int hour = picker.getHour();
                int minute = picker.getMinute();
                String hourText;
                String minuteText;
                hourText = hour < 10 ? "0" + String.valueOf(hour) : String.valueOf(hour);
                minuteText = minute < 10 ? "0" + String.valueOf(minute) : String.valueOf(minute);
                gameNightTime = hourText + ":" + minuteText;
            });
        });

        btnInsertGameNightData.setOnClickListener(v -> createNewGameNight());

        supa = new SupabaseClient(this);
    }

    private void createNewGameNight() {
        try {
            LocalDate.parse(gameNightDate.getText().toString(), formatter);
            java.time.format.DateTimeFormatter df =
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
            java.time.LocalDate d = java.time.LocalDate.parse(gameNightDate.getText().toString(), df);
            java.time.LocalTime t = java.time.LocalTime.parse(gameNightTime); // "HH:mm"
            java.time.ZonedDateTime zdt =
                    java.time.ZonedDateTime.of(d, t, java.time.ZoneId.systemDefault());

            if (zdt.isBefore(java.time.ZonedDateTime.now())) {
                Toast.makeText(this, "Bitte einen Termin in der Zukunft anlegen!", Toast.LENGTH_SHORT).show();
                return;
            }

            insertNewGameNightData(zdt);
        } catch (DateTimeParseException e) {
            Toast.makeText(CreateGameNightActivity.this,"Ungültiges Datum! Bitte Format TT.MM.JJJJ verwenden.",Toast.LENGTH_SHORT).show();
        }
    }

    private void insertNewGameNightData(java.time.ZonedDateTime zdt) {
        final String dateStr = gameNightDate.getText().toString();
        final String timeStr = gameNightTime;

        io.execute(() -> {
            try {
                String spielerJson = supa.selectAll("Spieler");
                com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(spielerJson);
                Spieler[] spielerArray = gson.fromJson(root, Spieler[].class);
                if (spielerArray == null || spielerArray.length == 0)
                    throw new IllegalStateException("Keine Spieler gefunden.");

                String gastgeberJson = supa.selectLastGastgeberView();
                String nextGastgeber = spielerArray[0].name;

                com.google.gson.JsonElement gastgeberRoot = com.google.gson.JsonParser.parseString(gastgeberJson);
                com.google.gson.JsonArray spielterminArr = gastgeberRoot.getAsJsonArray();

                if (spielterminArr.size() > 0 && spielterminArr.get(0).isJsonObject()) {
                    com.google.gson.JsonObject obj = spielterminArr.get(0).getAsJsonObject();
                    if (obj.has("gastgeber_name") && !obj.get("gastgeber_name").isJsonNull()) {
                        nextGastgeber = obj.get("gastgeber_name").getAsString();
                        for (int i = 0; i < spielerArray.length; i++) {
                            if (nextGastgeber.equals(spielerArray[i].name)) {
                                int idx = (i + 1) % spielerArray.length;
                                nextGastgeber = spielerArray[idx].name;
                                break;
                            }
                        }
                    }
                }
                String isoUtc = zdt.toInstant().toString(); // => "2025-09-27T16:00:00Z"

                com.google.gson.JsonObject spieltermin = new com.google.gson.JsonObject();
                spieltermin.addProperty("gastgeber_name", nextGastgeber);
                spieltermin.addProperty("termin", isoUtc);

                String resp = supa.insert("Spieltermin", spieltermin);

                JsonArray arr = JsonParser.parseString(resp).getAsJsonArray();
                JsonObject row = arr.get(0).getAsJsonObject();
                String host   = row.get("gastgeber_name").getAsString();
                String termin = row.get("termin").getAsString();

                Intent data = new Intent();
                data.putExtra("created_host", host);
                data.putExtra("created_termin", termin);

                main.post(() -> {
                    android.widget.Toast.makeText(this,
                            "Spieltermin angelegt für " + host + " am " + dateStr + " " + timeStr,
                            android.widget.Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK, data);
                    finish();
                });

            } catch (Exception e) {
                main.post(() ->
                        android.widget.Toast.makeText(this, "Fehler: " + e.getClass().getSimpleName(),
                                android.widget.Toast.LENGTH_LONG).show()
                );
                android.util.Log.e("insertNewGameNightData", "Unerwarteter Fehler", e);
            }
        });
    }
}

