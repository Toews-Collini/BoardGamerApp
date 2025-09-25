package com.example.boardgamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private SupabaseClient supa;
    private ExecutorService io = Executors.newSingleThreadExecutor();

    private EditText inputEmail, inputPassword;
    private Button btnRegister;
    private Button btnSignIn;
    private Button btnPasswordForgotten;

    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supa = new SupabaseClient();

        inputEmail = findViewById(R.id.editInputEmail);
        inputPassword = findViewById(R.id.editInputPassword);
       // btnRegister = findViewById(R.id.btnRegister);
       // progress = findViewById(R.id.progress);
        btnSignIn = findViewById(R.id.loginButton);
       // btnPasswordForgotten = findViewById(R.id.btnPasswordForgotten);

      //  btnRegister.setOnClickListener(v -> registerUser());
        btnSignIn.setOnClickListener(v -> signIn());
      //  btnPasswordForgotten.setOnClickListener(v -> loadPasswordForgottenActivity());
    }

    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String value = result.getData().getStringExtra("result");
                }
            });

    private void signIn() {
        final String email = inputEmail.getText().toString().trim();
        final String password = inputPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "E-Mail und Passwort eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        io.execute(() -> {
            try {
                // 1) Signup aufrufen
                boolean signedIn = supa.signInWithPassword(email, password);

                runOnUiThread(() -> {
                    if (signedIn) {
                        Toast.makeText(this,"Anmeldung erfolgreich",Toast.LENGTH_SHORT).show();
                        // Optional: Direkt einloggen (falls E-Mail-Bestätigung deaktiviert ist)
                    } else {
                        setLoading(false);
                        Toast.makeText(this,"Anmeldung fehlgeschlagen!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Netzwerk-/Serverfehler", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void registerUser() {
        final String email = inputEmail.getText().toString().trim();
        final String password = inputPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "E-Mail und Passwort eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        io.execute(() -> {
            try {
                // 1) Signup aufrufen
                boolean signedUp = supa.signUp(email, password);

                runOnUiThread(() -> {
                    if (signedUp) {
                        Toast.makeText(this,"Registrierung erfolgreich",Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(this, SignUpActivity.class);
                        i.putExtra("email", inputEmail.getText().toString().trim());
                        i.putExtra("password", inputPassword.getText().toString());
                        setResult(Activity.RESULT_OK, i);
                        launcher.launch(i);
                    } else {
                        setLoading(false);
                        if (supa.errorCode == 401 || supa.errorCode == 403) {
                            Toast.makeText(this,"Header/Key falsch oder falsche Projekt-URL", Toast.LENGTH_SHORT).show();
                        }
                        if (supa.errorCode == 400) {
                            Toast.makeText(this,"E-Mailadresse schon vorhanden!", Toast.LENGTH_SHORT).show();
                        }
                        if (supa.errorCode == 422) {
                            Toast.makeText(this,"Email-Adresse bereits vergeben oder Passwort zu kurz (mind. 6 Zeichen)", Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(this,"Registrierung fehlgeschlagen!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Netzwerk-/Serverfehler", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}

/*
    ArrayList<Spieleabend> anzeigenNächsteSpieleabende() {
        ArrayList<Spieleabend> spieleabendListe = new ArrayList<>();

        return spieleabendListe;
    }

    void befürwortenSpielFürSpieleabend(String spiel, Spieler spieler, Spieleabend spielabend) {
        spielabend.spielListe.putIfAbsent(spiel, new HashSet<>());
        spielabend.spielListe.get(spiel).add(spieler);
    }

    void absagenSpielFürSpieleabend(String spiel, Spieler spieler, Spieleabend spielabend) {
        spielabend.spielListe.get(spiel).remove(spieler);
    }

    void bewertenGastgeber(Spieler spieler, int sterne, String kommentar, Spieleabend spielabend) {
        Bewertung bewertung = new Bewertung();
        bewertung.sterne = sterne;
        bewertung.bewertung = kommentar;
        spielabend.gastgeberBewertung.put(spieler, bewertung);
    }

    void bewertenEssen(Spieler spieler, int sterne, String kommentar, Spieleabend spielabend) {
        Bewertung bewertung = new Bewertung();
        bewertung.sterne = sterne;
        bewertung.bewertung = kommentar;
        spielabend.essenBewertung.put(spieler, bewertung);
    }

    void bewertenAbend(Spieler spieler, int sterne, String kommentar, Spieleabend spielabend) {
        Bewertung bewertung = new Bewertung();
        bewertung.sterne = sterne;
        bewertung.bewertung = kommentar;
        spielabend.abendBewertung.put(spieler, bewertung);
    }

    void sendenSpielerNachricht(Spieler spieler, String nachricht) {
        spieler.nachrichten.add(nachricht);
    }

    ArrayList<String> anzeigenSpielerNachrichten(Spieler spieler) {
        return spieler.nachrichten;
    }
}

 */