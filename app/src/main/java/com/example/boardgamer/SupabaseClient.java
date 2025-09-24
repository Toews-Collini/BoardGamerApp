package com.example.boardgamer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.util.Log;

public class SupabaseClient {
    public static final String baseUrl = "https://bnykmtklumsygvtbbbry.supabase.co";           // https://<project>.supabase.co
    public static final String anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJueWttdGtsdW1zeWd2dGJiYnJ5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQwNTM0MDcsImV4cCI6MjA2OTYyOTQwN30.nkhUxiBFRcybP_Ted_l6SqZFT6VHQ1XhLIc6RPT4JhA";           // public anon key
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    // Session
    private static String accessToken;             // set after login
    private static String refreshToken;            // set after login

    public static int errorCode = 0;
    public String lastErrorBody = "";

    // Email/Password Login -> /auth/v1/token?grant_type=password
    public boolean signInWithPassword(String email, String password) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/auth/v1/token")
                .newBuilder()
                .addQueryParameter("grant_type", "password")
                .build();

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
            if (!res.isSuccessful()) {
                errorCode = res.code();
                return false;
            }
            JsonObject json = gson.fromJson(res.body().string(), JsonObject.class);
            this.accessToken = json.get("access_token").getAsString();
            this.refreshToken = json.get("refresh_token").getAsString();
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
            // GoTrue gibt 200 oder 201 bei Erfolg zurück (je nach Konfiguration)
            if (res.isSuccessful()) {
                return true;
            } else {
                // Typische Fehlertexte: already registered, password too short, etc.
                return false;
            }
        }
    }

    // Refresh -> /auth/v1/token?grant_type=refresh_token
    public boolean refreshSession() throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/auth/v1/token")
                .newBuilder()
                .addQueryParameter("grant_type", "refresh_token")
                .build();

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
            this.accessToken = json.get("access_token").getAsString();
            this.refreshToken = json.get("refresh_token").getAsString();
            return true;
        }
    }

    // ---------- DATABASE (PostgREST) ----------

    // SELECT: /rest/v1/<table>?select=*
    public String selectAll(String table) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/" + table)
                .newBuilder()
                .addQueryParameter("select", "*")
                .build();

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("Select failed: " + res.code());
            return res.body().string(); // JSON Array
        }
    }

    // INSERT: /rest/v1/<table>
    public String insert(String table, JsonObject row) throws IOException {
        String url = baseUrl + "/rest/v1/" + table;

        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
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
                .addHeader("Authorization", "Bearer " + accessToken)
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
                .addHeader("Authorization", "Bearer " + accessToken)
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
                .addHeader("Authorization", "Bearer " + accessToken)
                .delete()
                .build();

        try (Response res = http.newCall(req).execute()) {
            this.errorCode = res.code();
            if (!res.isSuccessful()) {
                throw new IOException("Delete " + table + " failed: " + res.code() + " / " + (res.body()!=null?res.body().string():""));
            }
        }
    }

    public String callRpc(String function, com.google.gson.JsonObject args) throws IOException {
        String url = baseUrl + "/rest/v1/rpc/" + function;
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create(args.toString(), okhttp3.MediaType.get("application/json")))
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (okhttp3.Response res = http.newCall(req).execute()) {
            String body = res.body() != null ? res.body().string() : "";
            this.errorCode = res.code();
            if (!res.isSuccessful()) {
                this.lastErrorBody = body;
                throw new IOException("RPC " + function + " failed: " + res.code() + " / " + body);
            }
            return body; // z.B. {"spieler_id":123,"adresse_id":456}
        }
    }
}
