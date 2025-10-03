package com.example.boardgamer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class SignUpActivity extends AppCompatActivity {
    private ExecutorService io = Executors.newSingleThreadExecutor();

    private SupabaseClient supa;

    String email;
    String password;
    EditText name;
    EditText plz;
    EditText city;
    EditText street;
    Button btnSignUp;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Intent intent = getIntent();
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");

        name = findViewById(R.id.inputNameSignUp);
        plz = findViewById(R.id.inputPLZSignUp);
        city = findViewById(R.id.inputCitySignUp);
        street = findViewById(R.id.inputStreetSignUp);
        btnSignUp = findViewById(R.id.btnSignUp);

        supa = new SupabaseClient();

        btnSignUp.setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty() || plz.getText().toString().trim().isEmpty() || city.getText().toString().trim().isEmpty() || street.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Bitte alle Felder ausfüllen!", Toast.LENGTH_SHORT).show();
                Log.v("Adresse","Bitte alle Felder ausfüllen!");
                return;
            }

            signUp();
        });
    }

    public void signUp() {
        io.execute(() -> {
            try {
                 // 1) Signup aufrufen
                boolean signedIn = supa.signInWithPassword(email, password);

                runOnUiThread(() -> {
                    if (signedIn) {
                        Toast.makeText(this,"Anmeldung erfolgreich",Toast.LENGTH_SHORT).show();
                        // Optional: Direkt einloggen (falls E-Mail-Bestätigung deaktiviert ist)
                    } else {
                        Toast.makeText(this,"Anmeldung fehlgeschlagen!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Netzwerk-/Serverfehler", Toast.LENGTH_SHORT).show();
                });
            }

            try {
                // RPC-Argumente
                com.google.gson.JsonObject args = new com.google.gson.JsonObject();
                args.addProperty("p_name", name.getText().toString().trim());
                args.addProperty("p_email", email);
                args.addProperty("p_plz", plz.getText().toString().trim());
                args.addProperty("p_ort", city.getText().toString().trim());
                args.addProperty("p_strasse", street.getText().toString().trim());

                // 3) Ein einzelner Call: erstellt Adresse + Spieler in einer Transaktion
                String resultJson = supa.callRpc("create_spieler_mit_adresse", args);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Profil angelegt", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, HomeActivity.class);
                    launcher.launch(i);
                });
            } catch (IOException e) {
                final int code = supa.errorCode;
                final String body = supa.lastErrorBody;
                runOnUiThread(() -> {
                    Toast.makeText(this,"Anlegen des Profils fehlgeschlagen!",Toast.LENGTH_LONG).show();
                    Log.v("Login", "(" + code + "): " + body);
                        if (SupabaseClient.errorCode == 409) {
                            Toast.makeText(this, "Name ist bereits vergeben!", Toast.LENGTH_SHORT).show();
                        }
                    }
                );
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String value = result.getData().getStringExtra("result");
                }
            });
}