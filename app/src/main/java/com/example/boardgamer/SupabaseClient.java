package com.example.boardgamer;

import static androidx.core.content.ContentProviderCompat.requireContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.content.Context;
import android.util.Log;

public final class SupabaseClient {
    public static final String baseUrl = "https://bnykmtklumsygvtbbbry.supabase.co";           // https://<project>.supabase.co
    public static final String anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJueWttdGtsdW1zeWd2dGJiYnJ5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQwNTM0MDcsImV4cCI6MjA2OTYyOTQwN30.nkhUxiBFRcybP_Ted_l6SqZFT6VHQ1XhLIc6RPT4JhA";
    public static String userEmail = "";
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Context appCtx;                   // <-- Context für AuthStore

    // NICHT static – Instanzzustand ist sauberer
    public String accessToken;
    private String refreshToken;

    public static int errorCode = 0;
    public String lastErrorBody = "";

    public SupabaseClient(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        // Beim Erstellen Token aus dem Store laden:
        this.accessToken  = AuthStore.loadAccess(this.appCtx);
        this.refreshToken = AuthStore.loadRefresh(this.appCtx);
    }

    public void setAccessToken(String token) { this.accessToken = token; }
    public boolean isAuthenticated() { return accessToken != null && !accessToken.isEmpty(); }

    private String authValue() {
        if (accessToken == null || accessToken.isEmpty())
            throw new IllegalStateException("Kein User-Access-Token – bitte einloggen.");
        if (accessToken.split("\\.").length != 3)
            throw new IllegalStateException("Ungültiges Access-Token-Format.");
        return "Bearer " + accessToken;
    }

    public boolean signInWithPassword(String email, String password) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/auth/v1/token")
                .newBuilder().addQueryParameter("grant_type", "password").build();

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) { errorCode = res.code(); return false; }

            JsonObject json = gson.fromJson(res.body().string(), JsonObject.class);
            this.accessToken  = json.get("access_token").getAsString();
            this.refreshToken = json.get("refresh_token").getAsString();

            // ➜ Tokens dauerhaft speichern
            AuthStore.saveTokens(appCtx, accessToken, refreshToken);

            userEmail = email;
            return true;
        }
    }

    public boolean refreshSession() throws IOException {
        if (refreshToken == null || refreshToken.isEmpty()) return false;

        HttpUrl url = HttpUrl.parse(baseUrl + "/auth/v1/token")
                .newBuilder().addQueryParameter("grant_type", "refresh_token").build();

        JsonObject body = new JsonObject();
        body.addProperty("refresh_token", refreshToken);

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) return false;

            JsonObject json = gson.fromJson(res.body().string(), JsonObject.class);
            this.accessToken  = json.get("access_token").getAsString();
            this.refreshToken = json.get("refresh_token").getAsString();

            // ➜ nach Refresh wieder speichern
            AuthStore.saveTokens(appCtx, accessToken, refreshToken);
            return true;
        }
    }

    // Signup -> /auth/v1/signup
    public boolean signUp(String email, String password) throws IOException {
        String url = baseUrl + "/auth/v1/signup";
        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            Log.d("SupabaseClient", "signUp status=" + res.code() + " body=" + body);
            errorCode = res.code();
            if (res.isSuccessful()) {
                return true;
            } else {
                return false;
            }
        }
    }

    public String getSpielerByEmail(String email) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/Spieler")
                .newBuilder()
                .addQueryParameter("email", "eq." + email)   // <— genau filtern
                .addQueryParameter("select", "name,email")   // nur was du brauchst
                .build();

        Request req = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())     // <— USER TOKEN!
                .addHeader("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", "GET Spieler by email -> " + res.code() + " body=" + body);
            errorCode = res.code();
            if (!res.isSuccessful()) throw new IOException("GET Spieler failed: " + res.code() + " / " + body);
            return body;
        }
    }



    // Refresh -> /auth/v1/token?grant_type=refresh_token

    // ---------- DATABASE (PostgREST) ----------

    public String selectAll(String table) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/" + table)
                .newBuilder()
                .addQueryParameter("select", "*")
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            errorCode = res.code();
            android.util.Log.d("SupabaseClient SelectAll",
                    "SELECT " + table + " -> status=" + res.code() + " body=" + body);

            if (!res.isSuccessful()) {
                this.lastErrorBody = body;
                throw new IOException("Select " + table + " failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }
    public String insert(String table, JsonObject row) throws IOException {
        String url = baseUrl + "/rest/v1/" + table;

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(RequestBody.create(row.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            errorCode = res.code();
            android.util.Log.d("SupabaseClient",
                    "INSERT " + table + " -> status=" + res.code() + " body=" + body);

            if (!res.isSuccessful()) {
                this.lastErrorBody = body;
                throw new IOException("Insert " + table + " failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String updateById(String table, String idColumn, long id, JsonObject patch) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/" + table)
                .newBuilder()
                .addQueryParameter(idColumn, "eq." + id)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(RequestBody.create(patch.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            this.errorCode = res.code();
            if (!res.isSuccessful()) {
                throw new IOException("Update " + table + " failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String updateById(String table, String idColumn, String pk, JsonObject patch) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/" + table)
                .newBuilder()
                .addQueryParameter(idColumn, "eq." + pk)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(RequestBody.create(patch.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            this.errorCode = res.code();
            if (!res.isSuccessful()) {
                throw new IOException("Update " + table + " failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public void deleteById(String table, String idColumn, long id) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/" + table)
                .newBuilder()
                .addQueryParameter(idColumn, "eq." + id)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .delete()
                .build();

        try (Response res = http.newCall(req).execute()) {
            this.errorCode = res.code();
            if (!res.isSuccessful()) {
                throw new IOException("Delete " + table + " failed: " + res.code() + " / " + (res.body() != null ? res.body().string() : ""));
            }
        }
    }

    public String callRpc(String function, com.google.gson.JsonObject args) throws IOException {
        String url = baseUrl + "/rest/v1/rpc/" + function;
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create(args.toString(), okhttp3.MediaType.get("application/json")))
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            this.errorCode = res.code();
            if (!res.isSuccessful()) {
                this.lastErrorBody = body;
                throw new IOException("RPC " + function + " failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String selectHomeActivityView() throws IOException {
        okhttp3.HttpUrl url = okhttp3.HttpUrl
                .parse(baseUrl + "/rest/v1/v_spieler_adresse_termin")
                .newBuilder()
                .addQueryParameter("select", "name,plz,ort,strasse,id,termin") // optional, aber klarer
                .build();

        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseClient.anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (okhttp3.Response res = http.newCall(req).execute()) {
            this.errorCode = res.code();
            String body = (res.body() != null) ? res.body().string() : "";

            if (!res.isSuccessful()) {
                this.lastErrorBody = body;
                throw new IOException("HTTP " + res.code() + " on GET v_spieler_adresse_termin");
            }

            this.lastErrorBody = null;
            return body;
        }
    }

    public String selectLastGastgeberView() throws IOException {
        okhttp3.HttpUrl url = okhttp3.HttpUrl
                .parse(baseUrl + "/rest/v1/v_latest_spieltermin_gastgeber")
                .newBuilder()
                .addQueryParameter("select", "gastgeber_name")
                .build();

        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseClient.anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (okhttp3.Response res = http.newCall(req).execute()) {
            this.errorCode = res.code();
            String body = (res.body() != null) ? res.body().string() : "";

            if (!res.isSuccessful()) {
                this.lastErrorBody = body;
                throw new IOException("HTTP " + res.code() + " on GET v_latest_spieltermin_gastgeber");
            }

            this.lastErrorBody = null;
            return body;
        }
    }

    public String upsertMany(String table, com.google.gson.JsonArray rows, String conflictTarget) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/" + table)
                .newBuilder()
                .addQueryParameter("on_conflict", conflictTarget)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation,resolution=merge-duplicates")
                .post(RequestBody.create(rows.toString(), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", "UPSERT " + table + " -> " + res.code() + " body=" + body);
            if (!res.isSuccessful()) {
                throw new IOException("Upsert " + table + " failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String getVorgeschlageneSpieleById(long id) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/Vorgeschlagene_Spiele")
                .newBuilder()
                .addQueryParameter("select", "*")
                .addQueryParameter("spieltermin_id", "eq." + id)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", res.code() + " body=" + body);
            if (!res.isSuccessful()) {
                throw new IOException("Select failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String getGespielteSpieleByIdAndPlayer(long id, String player) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/Gespielte_Spiele")
                .newBuilder()
                .addQueryParameter("select", "*")
                .addQueryParameter("spieltermin_id", "eq." + id)
                .addQueryParameter("spieler_name", "eq." + player)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", res.code() + " body=" + body);
            if (!res.isSuccessful()) {
                throw new IOException("Select failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String updateGespielteSpiele(long spieltermin_id, long spiel_id, String spieler_name, int spielerstimme_id) throws IOException {
        // RPC-Args exakt wie die Funktionsparameter heißen!
        com.google.gson.JsonObject args = new com.google.gson.JsonObject();
        args.addProperty("termin_id", spieltermin_id);
        args.addProperty("game_id",   spiel_id);
        args.addProperty("player_name", spieler_name);
        args.addProperty("vote",        spielerstimme_id);

        String body = callRpc("update_gespielte_spiele", args);  // POST /rest/v1/rpc/update_gespielte_spiele
        android.util.Log.d("RPC:update_gespielte_spiele", body);
        return body;
    }

    public String getRatingByIdAndPlayer(long id, String player) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/Spieleabend")
                .newBuilder()
                .addQueryParameter("select", "*")
                .addQueryParameter("spieltermin_id", "eq." + id)
                .addQueryParameter("spieler_name", "eq." + player)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", res.code() + " body=" + body);
            if (!res.isSuccessful()) {
                throw new IOException("Select failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String updateSpieleabend(long spieltermin_id, String spieler_name, int host_stars, String host_comment, int food_stars, String food_comment, int evening_stars, String evening_comment) throws IOException {
        com.google.gson.JsonObject args = new com.google.gson.JsonObject();
        args.addProperty("termin_id", spieltermin_id);
        args.addProperty("player_name", spieler_name);
        args.addProperty("host_stars", host_stars);
        args.addProperty("host_comment", host_comment);
        args.addProperty("food_stars", food_stars);
        args.addProperty("food_comment", food_comment);
        args.addProperty("evening_stars", evening_stars);
        args.addProperty("evening_comment", evening_comment);

        String body = callRpc("update_spieleabend", args);  // POST /rest/v1/rpc/update_gespielte_spiele
        android.util.Log.d("RPC:update_spieleabend", body);
        return body;
    }

    public String getFoodChoiceByIdAndPlayer(long id, String player) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/Gewaehlte_Essensrichtung")
                .newBuilder()
                .addQueryParameter("select", "*")
                .addQueryParameter("spieltermin_id", "eq." + id)
                .addQueryParameter("spieler_name", "eq." + player)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", res.code() + " body=" + body);
            if (!res.isSuccessful()) {
                throw new IOException("Select failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }

    public String updateGewaehlteEssensrichtung(long spieltermin_id, String spieler_name, int food_choice) throws IOException {
        com.google.gson.JsonObject args = new com.google.gson.JsonObject();
        args.addProperty("termin_id", spieltermin_id);
        args.addProperty("player_name", spieler_name);
        args.addProperty("food_choice", food_choice);

        String body = callRpc("update_gewaehlte_essensrichtung", args);  // POST /rest/v1/rpc/update_gewaehlte_essensrichtung
        android.util.Log.d("RPC:update_gewaehlte_essensrichtung", body);
        return body;
    }

    public String getGewaehlteEssenrichtungById(long id) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/Gewaehlte_Essensrichtung")
                .newBuilder()
                .addQueryParameter("select", "*")
                .addQueryParameter("spieltermin_id", "eq." + id)
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", authValue())
                .addHeader("Accept", "application/json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            android.util.Log.d("SupabaseClient", res.code() + " body=" + body);
            if (!res.isSuccessful()) {
                throw new IOException("Select failed: " + res.code() + " / " + body);
            }
            return body;
        }
    }
}

