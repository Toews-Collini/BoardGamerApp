package com.example.boardgamer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private SupabaseClient supa;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private EditText inputEmail, inputPassword;
    private Button btnRegister;
    private Button btnSignIn;

    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supa = new SupabaseClient(this);

        inputEmail = findViewById(R.id.editInputEmail);
        inputPassword = findViewById(R.id.editInputPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);
        btnSignIn = findViewById(R.id.loginButton);

        btnRegister.setOnClickListener(v -> registerUser());
        btnSignIn.setOnClickListener(v -> signIn());
/*
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
*/
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
            Toast.makeText(this, getString(R.string.error_invalidLogin), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        io.execute(() -> {
            try {
                boolean signedIn = supa.signInWithPassword(email, password);

                runOnUiThread(() -> {
                    if (signedIn) {
                        Intent i = new Intent(this, HomeActivity.class);
                        setResult(Activity.RESULT_OK, i);
                        launcher.launch(i);
                        Toast.makeText(this, getString(R.string.success_login_successful), Toast.LENGTH_SHORT).show();
                    } else {
                        setLoading(false);
                        Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_network_server), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void registerUser() {
        final String email = inputEmail.getText().toString().trim();
        final String password = inputPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_invalidLogin), Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        io.execute(() -> {
            try {
                boolean signedUp = supa.signUp(email, password);

                runOnUiThread(() -> {
                    if (signedUp) {
                        Toast.makeText(this, getString(R.string.success_registration_successful), Toast.LENGTH_SHORT).show();

                        Intent i = new Intent(this, SignUpActivity.class);
                        i.putExtra("email", inputEmail.getText().toString().trim());
                        i.putExtra("password", inputPassword.getText().toString());
                        setResult(Activity.RESULT_OK, i);
                        launcher.launch(i);
                    } else {
                        setLoading(false);
                        if (supa.errorCode == 401 || supa.errorCode == 403) {
                            Toast.makeText(this, getString(R.string.url), Toast.LENGTH_SHORT).show();
                        }
                        if (supa.errorCode == 400) {
                            Toast.makeText(this, getString(R.string.error_mail_already_exists), Toast.LENGTH_SHORT).show();
                        }
                        if (supa.errorCode == 422) {
                            Toast.makeText(this, getString(R.string.error_mail), Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(this, getString(R.string.error_registration_failed), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.error_network_server), Toast.LENGTH_SHORT).show();
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
