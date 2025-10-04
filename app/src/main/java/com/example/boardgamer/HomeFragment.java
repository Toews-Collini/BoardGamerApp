package com.example.boardgamer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeFragment extends Fragment {
    private static final String TAG = "SpieleabendViewJson";
    private static final java.time.ZoneId APP_ZONE = java.time.ZoneId.of("Europe/Berlin");
    private static final java.time.format.DateTimeFormatter DISP_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Gson gson = new Gson();

    private SupabaseClient supa;
    private SpieleabendView[] data = new SpieleabendView[0];
    private int index = 0;

    // Views
    private TextView gameNightDate, gameNightLocation, gameNightHost;
    private TextView nextGameNightDate1, nextGameNightDate2, nextGameNightDate3;
    private TextView nextGameNightLocation1, nextGameNightLocation2, nextGameNightLocation3;
    private TextView nextGameNightHost1, nextGameNightHost2, nextGameNightHost3;


    private Button btnNewGameNight;
    // private ImageButton imgBtnPrevious;
   // private ImageButton imgBtnNext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supa = new SupabaseClient();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_home, container, false);
    }

    @Override
    public void onViewCreated(@androidx.annotation.NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // >> HIER Views binden (nicht in onCreate)
        gameNightDate     = view.findViewById(R.id.gameNightDate);
        gameNightLocation = view.findViewById(R.id.gameNightLocation);
        gameNightHost     = view.findViewById(R.id.gameNightHost);
        nextGameNightDate1 = view.findViewById(R.id.gameNight1);
        nextGameNightLocation1 = view.findViewById(R.id.gameNight2);
        nextGameNightHost1 = view.findViewById(R.id.gameNight3);
        nextGameNightDate2 = view.findViewById(R.id.tvDate2);
        nextGameNightLocation2 = view.findViewById(R.id.tvLocation2);
        nextGameNightHost2 = view.findViewById(R.id.tvHost2);
        nextGameNightDate3 = view.findViewById(R.id.tvDate3);
        nextGameNightLocation3 = view.findViewById(R.id.tvLocation3);
        nextGameNightHost3 = view.findViewById(R.id.tvHost3);

        btnNewGameNight   = view.findViewById(R.id.newGameNightButton);
       // imgBtnPrevious = view.findViewById(R.id.imgBtnPrevious);
       // imgBtnNext = view.findViewById(R.id.imgBtnNext);

        btnNewGameNight.setOnClickListener(v -> createGameNight());

       /* imgBtnPrevious.setOnClickListener(v -> {
            if (data.length == 0) return;
            index = Math.max(0, index - 1);
            render(false); // <-- vergangene Termine zulassen
        });

        imgBtnNext.setOnClickListener(v -> {
            if (data.length == 0) return;
            index = Math.min(data.length - 1, index + 1);
            render(true);  // <-- bei Next Zukunft erzwingen (überspringt ggf. vergangene)
        }); */

        btnNewGameNight.setOnClickListener(v -> createGameNight());

        loadData(); // EINMAL vom Server holen
    }

    // 1) Laden (einmal, im Hintergrund), im Erfolg: initialen Index bestimmen, rendern
    private void loadData() {
        io.execute(() -> {
            try {
                String json = supa.selectHomeActivityView();
                SpieleabendView[] arr = gson.fromJson(
                        com.google.gson.JsonParser.parseString(json),
                        SpieleabendView[].class
                );
                if (arr == null) arr = new SpieleabendView[0];

                // robust: client-seitig sortieren nach Termin aufsteigend
                java.util.Arrays.sort(arr, (a, b) -> {
                    java.time.Instant ia = parseInstant(a != null ? a.termin : null);
                    java.time.Instant ib = parseInstant(b != null ? b.termin : null);
                    if (ia == null && ib == null) return 0;
                    if (ia == null) return 1;   // nulls ans Ende
                    if (ib == null) return -1;
                    return ia.compareTo(ib);
                });

                final SpieleabendView[] finalArr = arr;
                main.post(() -> {
                    data = finalArr;
                    index = computeInitialIndex(data);   // erster Termin > jetzt (oder Fallback)
                    render(true);                        // <-- Zukunft erzwingen
                });            } catch (Exception e) {
                main.post(() -> {
                    if (!isAdded()) return;
                    android.widget.Toast.makeText(requireContext(),
                            "Laden fehlgeschlagen", android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ========== 2) Ersten *zukünftigen* Index finden ==========
    private int computeInitialIndex(SpieleabendView[] arr) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(APP_ZONE);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null || arr[i].termin == null) continue;
            java.time.ZonedDateTime when = parseLocalIgnoringOffset(arr[i].termin);
            if (when != null && when.isAfter(now)) return i;   // strikt später
        }
        return Math.max(0, arr.length - 1);
    }

    private void ensureCurrentIsFuture() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(APP_ZONE);
        while (index < data.length) {
            SpieleabendView v = data[index];
            java.time.ZonedDateTime when = (v != null) ? parseLocalIgnoringOffset(v.termin) : null;
            if (when != null && when.isAfter(now)) break;
            index++;
        }
        if (index >= data.length) index = Math.max(0, data.length - 1);
    }

    private void render(boolean enforceFuture) {
        if (!isAdded()) return;
        if (data.length == 0) {
            gameNightDate.setText(""); gameNightLocation.setText(""); gameNightHost.setText("");
            nextGameNightDate1.setText(""); nextGameNightDate2.setText(""); nextGameNightDate3.setText("");
            nextGameNightLocation1.setText(""); nextGameNightLocation2.setText(""); nextGameNightLocation3.setText("");
            nextGameNightHost1.setText(""); nextGameNightHost2.setText(""); nextGameNightHost3.setText("");
            // imgBtnPrevious.setEnabled(false); imgBtnNext.setEnabled(false);
            return;
        }

        // NUR erzwingen, wenn gewünscht (Initial/Next), bei Previous NICHT!
        if (enforceFuture) ensureCurrentIsFuture();

        // clamp (falls ensure… Index ans Ende geschoben hat)
        if (index < 0) index = 0;
        if (index >= data.length) index = data.length - 1;

        // Hauptkarte
        SpieleabendView cur = data[index];
        gameNightDate.setText("Game Night Date: " + fmtLocal(cur.termin));
        gameNightLocation.setText("Game Night Location: " + cur.plz + " " + cur.ort + "; " + cur.strasse);
        gameNightHost.setText("Game Night Host: " + cur.name);

        // Nächste 3 relativ zu „index“
        setUpcomingText(nextGameNightDate1, nextGameNightLocation1, nextGameNightHost1, index + 1);
        setUpcomingText(nextGameNightDate2, nextGameNightLocation2, nextGameNightHost2, index + 2);
        setUpcomingText(nextGameNightDate3, nextGameNightLocation3, nextGameNightHost3, index + 3);

       // imgBtnPrevious.setEnabled(index > 0);
      //  imgBtnNext.setEnabled(index < data.length - 1);
    }

    // ========== 5) Parsing/Formatting (wie gehabt, leicht gehärtet) ==========
    private java.time.Instant parseInstant(String ts) {
        if (ts == null || ts.isEmpty()) return null;
        try { return java.time.OffsetDateTime.parse(ts).toInstant(); } // mit Z/Offset
        catch (java.time.format.DateTimeParseException e) {
            try { return java.time.LocalDateTime.parse(ts)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant(); } // ohne Offset -> lokal
            catch (Exception ignore) { return null; }
        }
    }

    private java.time.ZonedDateTime parseLocalIgnoringOffset(String ts) {
        if (ts == null || ts.isEmpty()) return null;
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(ts);
            return ldt.atZone(APP_ZONE);
        } catch (java.time.format.DateTimeParseException e) {
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(ts);
                java.time.LocalDateTime ldt = odt.toLocalDateTime(); // <— Offset drop
                return ldt.atZone(APP_ZONE);
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
        Intent i = new Intent(requireContext(), CreateGameNightActivity.class);
        launcher.launch(i);
    }

    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!isAdded() || result.getData() == null) return;
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
                    loadData(); // danach frisch laden
                }
            });

}
