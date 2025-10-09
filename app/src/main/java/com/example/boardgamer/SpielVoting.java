package com.example.boardgamer;

import com.google.gson.annotations.SerializedName;

public class SpielVoting {
    @SerializedName("spieltermin_id") public long spieltermin_id;
    @SerializedName("spiel_id") public long spiel_id;
    @SerializedName("spieler_name") public String spieler_name;
    @SerializedName("spielerstimme_id") public int spielerstimme_id;
}
