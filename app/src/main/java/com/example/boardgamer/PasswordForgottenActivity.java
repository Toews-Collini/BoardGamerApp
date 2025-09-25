/* package com.example.boardgamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class PasswordForgottenActivity extends AppCompatActivity {

    private OkHttpClient http;
    private EditText emailInput, otpInput, passwordInput;
    private ProgressBar progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_forgotten);

        http = new OkHttpClient();

        emailInput = findViewById(R.id.emailInput);
        otpInput = findViewById(R.id.otpInput);
        passwordInput = findViewById(R.id.passwordInput);
        progress = findViewById(R.id.progress);

        Button sendCodeBtn = findViewById(R.id.sendCodeBtn);
        Button resetBtn = findViewById(R.id.resetBtn);

        sendCodeBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                toast("Bitte E-Mail eingeben");
                return;
            }
            sendRecoveryEmail(email);
        });

        resetBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String otp = otpInput.getText().toString().trim();
            String newPw = passwordInput.getText().toString();

            if (email.isEmpty() || otp.length() != 6) {
                toast("E-Mail und 6-stelligen Code prüfen");
                return;
            }
            if (newPw.length() < 6) {
                toast("Passwort zu kurz (min. 6 Zeichen)");
                return;
            }
            verifyOtpAndUpdatePassword(email, otp, newPw);
        });
    }

    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String value = result.getData().getStringExtra("result");
                }
            });

    private void loadLogin() {
        Intent i = new Intent(this, MainActivity.class);
        launcher.launch(i);
    }

    private void sendRecoveryEmail(String email) {
        setLoading(true);
        String url = SupabaseClient.baseUrl + "/auth/v1/recover";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String body = "{\"email\":\"" + email + "\"}";

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, JSON))
                .addHeader("apikey", SupabaseClient.anonKey)
                .addHeader("Content-Type", "application/json")
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { ui(() -> { setLoading(false); toast("Netzwerkfehler: " + e.getMessage()); }); }
            @Override public void onResponse(Call call, Response res) {
                ui(() -> {
                    setLoading(false);
                    if (res.isSuccessful()) {
                        toast("Code per E-Mail verschickt (falls registriert).");
                    } else {
                        toast("Fehler beim Versenden: " + res.code());
                    }
                    res.close();
                });
            }
        });
    }

    private void verifyOtpAndUpdatePassword(String email, String otp, String newPassword) {
        setLoading(true);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        // 1) OTP verifizieren -> access_token holen
        String verifyUrl = SupabaseClient.baseUrl + "/auth/v1/verify";
        String verifyBody = "{\"type\":\"recovery\",\"email\":\"" + email + "\",\"token\":\"" + otp + "\"}";
        Request verifyReq = new Request.Builder()
                .url(verifyUrl)
                .post(RequestBody.create(verifyBody, JSON))
                .addHeader("apikey", SupabaseClient.anonKey)
                .addHeader("Content-Type", "application/json")
                .build();

        http.newCall(verifyReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { ui(() -> { setLoading(false); toast("Verify fehlgeschlagen: " + e.getMessage()); }); }
            @Override public void onResponse(Call call, Response res) {
                try (Response r = res) {
                    if (!r.isSuccessful()) {
                        String err = r.body() != null ? r.body().string() : "";
                        ui(() -> { setLoading(false); toast("Verify-Fehler: " + r.code() + " " + err); });
                        return;
                    }
                    String resp = r.body() != null ? r.body().string() : "{}";
                    String accessToken = new JSONObject(resp).optString("access_token", null);
                    if (accessToken == null || accessToken.isEmpty()) {
                        ui(() -> { setLoading(false); toast("Kein access_token erhalten."); });
                        return;
                    }
                    // 2) Passwort setzen
                    updatePassword(accessToken, newPassword);
                } catch (Exception e) {
                    ui(() -> { setLoading(false); toast("Antwortfehler: " + e.getMessage()); });
                }
            }
        });
    }

    private void updatePassword(String accessToken, String newPassword) {
        String url = SupabaseClient.baseUrl + "/auth/v1/user";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String body = "{\"password\":\"" + newPassword + "\"}";

        Request req = new Request.Builder()
                .url(url)
                .put(RequestBody.create(body, JSON))
                .addHeader("apikey", SupabaseClient.anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { ui(() -> { setLoading(false); toast("Update fehlgeschlagen: " + e.getMessage()); }); }
            @Override public void onResponse(Call call, Response res) {
                ui(() -> {
                    setLoading(false);
                    if (res.isSuccessful()) {
                        toast("Passwort aktualisiert. Bitte neu anmelden.");
                        finish();
                        loadLogin();
                    } else {
                        toast("Update-Fehler: " + res.code());
                    }
                    res.close();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void ui(Runnable r) { runOnUiThread(r); }
} */
