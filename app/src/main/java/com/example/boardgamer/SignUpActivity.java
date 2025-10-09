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

        supa = new SupabaseClient(this);

        btnSignUp.setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty() || plz.getText().toString().trim().isEmpty() || city.getText().toString().trim().isEmpty() || street.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.error_all_files_filled_out), Toast.LENGTH_SHORT).show();
                Log.v("Adresse",getString(R.string.error_all_files_filled_out));
                return;
            }

            signUp();
        });
    }

    public void signUp() {
        io.execute(() -> {
            try {
                boolean signedIn = supa.signInWithPassword(email, password);

                runOnUiThread(() -> {
                    if (signedIn) {
                        Toast.makeText(this,getString(R.string.success_login_successful),Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_network_server), Toast.LENGTH_SHORT).show();
                });
            }

            try {
                com.google.gson.JsonObject args = new com.google.gson.JsonObject();
                args.addProperty("p_name", name.getText().toString().trim());
                args.addProperty("p_email", email);
                args.addProperty("p_plz", plz.getText().toString().trim());
                args.addProperty("p_ort", city.getText().toString().trim());
                args.addProperty("p_strasse", street.getText().toString().trim());

                String resultJson = supa.callRpc("create_spieler_mit_adresse", args);

                runOnUiThread(() -> {
                    Intent i = new Intent(this, HomeActivity.class);
                    launcher.launch(i);
                    Toast.makeText(this, getString(R.string.success_profile_created), Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (IOException e) {
                final int code = supa.errorCode;
                final String body = supa.lastErrorBody;
                runOnUiThread(() -> {
                    Toast.makeText(this,getString(R.string.error_profile_creation_failed),Toast.LENGTH_LONG).show();
                    Log.v("Login", "(" + code + "): " + body);
                        if (SupabaseClient.errorCode == 409) {
                            Toast.makeText(this,getString(R.string.error_name_already_taken) , Toast.LENGTH_SHORT).show();
                        }
                    }
                );
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
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