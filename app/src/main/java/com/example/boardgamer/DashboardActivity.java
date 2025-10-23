package com.example.boardgamer;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;


import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardActivity extends AppCompatActivity {
    public static long termin_id;
    TextView gameNightDate;
    TextView gameNightLocation;
    TextView gameNightHost;

    CardView cvSuggestions;
    CardView cvVoting;
    CardView cvRating;
    CardView cvDinner;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

       BottomNavigationView bottomNavigation;

        Intent i = getIntent();
        termin_id = i.getLongExtra("termin_id", -1);
        String dateString = i.getStringExtra("termin_date");
        OffsetDateTime odt = OffsetDateTime.parse(dateString);
        ZonedDateTime zdt = odt.atZoneSameInstant(ZoneId.systemDefault());
        String formattedDate = zdt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        String location = i.getStringExtra("termin_location");
        String host = i.getStringExtra("termin_host");

       gameNightDate = findViewById(R.id.tvDashboardDate);
       gameNightLocation = findViewById(R.id.tvDashboardLocation);
       gameNightHost = findViewById(R.id.tvDashboardHost);

       cvSuggestions = findViewById(R.id.card1);
       cvVoting = findViewById(R.id.card2);
       cvRating = findViewById(R.id.card3);
       cvDinner = findViewById(R.id.card4);

       gameNightDate.setText((getString(R.string.gameNightDate) + formattedDate));
       gameNightLocation.setText((getString(R.string.gameNightLocation) + location));
       gameNightHost.setText((getString(R.string.gameNightHost) + host));

       cvSuggestions.setOnClickListener(v -> showSuggestions());
       cvVoting.setOnClickListener(v -> showVoting());
       cvRating.setOnClickListener(v -> showRating());
       cvDinner.setOnClickListener(v -> {
           if(host.equals(Spieler.appUserName)) {
               showFoodChoices();
           } else{
               showDinner();
           }
       });

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if(id == R.id.home) {
                finish();
            }
            else if(id == R.id.messages) {
                Intent intent = new Intent(this, MessagesActivity.class);
                startActivity(intent);
            }
            else if(id == R.id.settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            }
            return true;
        });
   }

    private void showSuggestions() {
        Intent i = new Intent(this, SuggestionsActivity.class);
        i.putExtra("spieltermin_id", termin_id);
        launcher.launch(i);
    }

    private void showVoting() {
        Intent i = new Intent(this, VotingActivity.class);
        i.putExtra("spieltermin_id", termin_id);
        launcher.launch(i);
    }

    private void showRating() {
        Intent i = new Intent(this, RatingActivity.class);
        i.putExtra("spieltermin_id", termin_id);
        launcher.launch(i);
    }

    private void showDinner() {
        Intent i = new Intent(this, FoodActivity.class);
        i.putExtra("spieltermin_id", termin_id);
        launcher.launch(i);
    }

    private void showFoodChoices() {
        Intent i = new Intent(this, FoodChoiceOverview.class);
        i.putExtra("spieltermin_id", termin_id);
        launcher.launch(i);
    }

    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            });
}
