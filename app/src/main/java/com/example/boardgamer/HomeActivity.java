package com.example.boardgamer;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.concurrent.ExecutorService;

public class HomeActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigation;
    private static final String TAG = "SpieleabendViewJson";
    private static final java.time.format.DateTimeFormatter DISP_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Gson gson = new Gson();
    private SupabaseClient supa;
    private SpieleabendView[] data = new SpieleabendView[0];
    private SpieleabendView currentSpieltermin = new SpieleabendView();
    private int index = 0;
    private int lastIndex = 0;
    private boolean setNotification = false;
    long alarmTimeInSeconds = 0;
    // Views
    private TextView gameNightDate, gameNightLocation, gameNightHost;
    private TextView nextGameNightDate1, nextGameNightDate2, nextGameNightDate3;
    private TextView nextGameNightLocation1, nextGameNightLocation2, nextGameNightLocation3;
    private TextView nextGameNightHost1, nextGameNightHost2, nextGameNightHost3;

    private Button btnViewDetails;
    private Button btnNewGameNight;
     private ImageView imgBtnPrevious;
     private ImageView imgBtnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        supa = new SupabaseClient(this);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
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

        gameNightDate = findViewById(R.id.gameNightDate);
        gameNightLocation = findViewById(R.id.gameNightLocation);
        gameNightHost = findViewById(R.id.gameNightHost);
        nextGameNightDate1 = findViewById(R.id.gameNight1);
        nextGameNightLocation1 = findViewById(R.id.gameNight2);
        nextGameNightHost1 = findViewById(R.id.gameNight3);
        nextGameNightDate2 = findViewById(R.id.tvDate2);
        nextGameNightLocation2 = findViewById(R.id.tvLocation2);
        nextGameNightHost2 = findViewById(R.id.tvHost2);
        nextGameNightDate3 = findViewById(R.id.tvDate3);
        nextGameNightLocation3 = findViewById(R.id.tvLocation3);
        nextGameNightHost3 = findViewById(R.id.tvHost3);

        btnViewDetails = findViewById(R.id.btnViewDetails);
        btnNewGameNight = findViewById(R.id.newGameNightButton);
        imgBtnPrevious = findViewById(R.id.btnBack);
        imgBtnNext = findViewById(R.id.btnNext);

        btnViewDetails.setOnClickListener(v -> showViewDetails());
        btnNewGameNight.setOnClickListener(v -> createGameNight());

        imgBtnPrevious.setOnClickListener(v -> {
            if (data.length == 0) return;
            index = Math.max(0, index - 1);
            render(false);
        });

        imgBtnNext.setOnClickListener(v -> {
            if (data.length == 0) return;
            index = Math.min(lastIndex, index + 1);
            render(false);
        });

        btnNewGameNight.setOnClickListener(v -> createGameNight());
        setAppUserName(() -> {
            currentSpieltermin.termin_id = -1;
            loadData();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        io.execute(() -> {
            try {
                String json = supa.selectHomeActivityView();
                SpieleabendView[] arr = gson.fromJson(
                        com.google.gson.JsonParser.parseString(json),
                        SpieleabendView[].class
                );
                if (arr == null) arr = new SpieleabendView[0];

                final SpieleabendView[] finalArr = arr;
                main.post(() -> {
                    data = finalArr;
                    index = computeInitialIndex(data);
                    render(true);
                });            } catch (Exception e) {
                main.post(() -> {
                    android.widget.Toast.makeText(this,
                            R.string.loading_failed, android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int computeInitialIndex(SpieleabendView[] arr) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null || arr[i].termin == null) continue;
            java.time.ZonedDateTime when = parseLocalIgnoringOffset(arr[i].termin);
            if (when != null && when.isAfter(now)) return i;
        }
        return Math.max(0, arr.length - 1);
    }

    private void ensureCurrentIsFuture() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        while (index < data.length) {
            SpieleabendView v = data[index];
            java.time.ZonedDateTime when = (v != null) ? parseLocalIgnoringOffset(v.termin) : null;
            if (when != null && when.isAfter(now)) {
                long nowInSeconds = now.toInstant().getEpochSecond();
                long WhenInseconds = when.toInstant().getEpochSecond();
                if (WhenInseconds >= nowInSeconds- (3600 * 24)) {
//                if (WhenInseconds - (3600 * 24) <= nowInSeconds) {
                    alarmTimeInSeconds = WhenInseconds;
                    setNotification = true;
                }
                lastIndex = index;
                break;
            }
            index++;
        }
        if (index >= data.length) {
            index = Math.max(0, data.length - 1);
        }
    }

    private void render(boolean enforceFuture) {
        if (data.length == 0) {
            gameNightDate.setText(""); gameNightLocation.setText(""); gameNightHost.setText("");
            nextGameNightDate1.setText(""); nextGameNightDate2.setText(""); nextGameNightDate3.setText("");
            nextGameNightLocation1.setText(""); nextGameNightLocation2.setText(""); nextGameNightLocation3.setText("");
            nextGameNightHost1.setText(""); nextGameNightHost2.setText(""); nextGameNightHost3.setText("");
            imgBtnPrevious.setEnabled(false);
            imgBtnNext.setEnabled(false);
            return;
        }

        if (enforceFuture) ensureCurrentIsFuture();

        if (index < 0) index = 0;
        if (index >= lastIndex) index = lastIndex;

        currentSpieltermin = data[index];
        gameNightDate.setText("Game Night Date: " + fmtLocal(currentSpieltermin.termin));
        gameNightLocation.setText("Game Night Location: " + currentSpieltermin.plz + " " + currentSpieltermin.ort + "; " + currentSpieltermin.strasse);
        gameNightHost.setText("Game Night Host: " + currentSpieltermin.name);

        setUpcomingText(nextGameNightDate1, nextGameNightLocation1, nextGameNightHost1, index + 1);
        setUpcomingText(nextGameNightDate2, nextGameNightLocation2, nextGameNightHost2, index + 2);
        setUpcomingText(nextGameNightDate3, nextGameNightLocation3, nextGameNightHost3, index + 3);

        maybeScheduleNotification();

        imgBtnPrevious.setEnabled(index > 0);
        imgBtnNext.setEnabled(index < lastIndex);
    }

    private java.time.ZonedDateTime parseLocalIgnoringOffset(String ts) {
        if (ts == null || ts.isEmpty()) return null;
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(ts);
            return ldt.atZone(java.time.ZoneId.systemDefault());
        } catch (java.time.format.DateTimeParseException e) {
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(ts);
                java.time.LocalDateTime ldt = odt.toLocalDateTime();
                return ldt.atZone(java.time.ZoneId.systemDefault());
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private String fmtLocal(String iso) {
        java.time.ZonedDateTime z = parseLocalIgnoringOffset(iso);
        return (z != null) ? z.format(DISP_FMT) : (iso == null ? "" : iso);
    }

    private void setUpcomingText(TextView tvDate, TextView tvLoc, TextView tvHost, int idx) {
        if (idx >= 0 && idx < data.length && data[idx] != null) {
            var v = data[idx];
            tvDate.setText("Game Night Date: " + fmtLocal(v.termin));
            tvLoc.setText("Game Night Location: " + v.plz + " " + v.ort + "; " + v.strasse);
            tvHost.setText("Game Night Host: " + v.name);
        } else {
            tvDate.setText(""); tvLoc.setText(""); tvHost.setText("");
        }
    }
    private void createGameNight() {
        Intent i = new Intent(this, CreateGameNightActivity.class);
        launcher.launch(i);
    }

    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    String host   = result.getData().getStringExtra("created_host");
                    String termin = result.getData().getStringExtra("created_termin");
                    try {
                        java.time.Instant t = java.time.OffsetDateTime.parse(termin).toInstant();
                        String display = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .format(t.atZone(java.time.ZoneId.systemDefault()));
                        gameNightDate.setText(display);
                    } catch (Exception ignore) {
                        gameNightDate.setText(termin);
                    }
                    gameNightHost.setText(host);
                    loadData();
                }
            });

    private void showViewDetails() {
        if(currentSpieltermin.termin_id != -1) {
            Intent i = new Intent(this, DashboardActivity.class);
            i.putExtra("termin_id", currentSpieltermin.termin_id);
            i.putExtra("termin_date", currentSpieltermin.termin);
            i.putExtra("termin_location", currentSpieltermin.plz + " " +
                    currentSpieltermin.ort + "; " + currentSpieltermin.strasse);
            i.putExtra("termin_host", currentSpieltermin.name);
            launcher.launch(i);
        } else {
            Toast.makeText(this, R.string.no_data, Toast.LENGTH_SHORT).show();
        }
    }

    private void setAppUserName(Runnable then) {
        io.execute(() -> {
            try {
                String appuserJson = supa.getSpielerByEmail(SupabaseClient.userEmail);
                Spieler[] arr = gson.fromJson(com.google.gson.JsonParser.parseString(appuserJson), Spieler[].class);
                String foundName = (arr != null && arr.length > 0) ? arr[0].name : null;

                main.post(() -> {
                    if (foundName != null) {
                        Spieler.appUserName = foundName;
                        then.run();
                    } else {
                        Toast.makeText(this, R.string.loading_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, R.string.loading_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void scheduleNotification() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 2001);
            return;
        }

        if (Build.VERSION.SDK_INT >= 31 && am != null && !am.canScheduleExactAlarms()) {
            Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(i);
            return;
        }

        long millis = (alarmTimeInSeconds + 10) * 1000;
//        long millis = ((alarmTimeInSeconds - 3600) * 1000);

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (am != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, millis, pi);
            }
        }
    }

    private void maybeScheduleNotification() {
//        if (!setNotification) return;

        io.execute(() -> {
            try {
                String essenJson = supa.getFoodChoiceByIdAndPlayer(
                        currentSpieltermin.termin_id, Spieler.appUserName
                );

                EssenWahl[] essenwahl = null;
                if (essenJson != null && !essenJson.isBlank()) {
                    essenwahl = gson.fromJson(JsonParser.parseString(essenJson), EssenWahl[].class);
                }

                final boolean shouldNotify = (essenwahl == null || essenwahl.length == 0);

                main.post(() -> {
                    if (shouldNotify) scheduleNotification();
                    setNotification = false;
                });
            } catch (Exception e) {
                main.post(() -> {
                    Toast.makeText(this, R.string.loading_failed, Toast.LENGTH_SHORT).show();
                    setNotification = false;
                });
            }
        });
    }
}
