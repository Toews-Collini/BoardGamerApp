package com.example.boardgamer;

import com.google.gson.annotations.SerializedName;

public class SpieleabendRating {
    @SerializedName("spieltermin_id") public long spieltermin_id;
    @SerializedName("spieler_name") public String spieler_name;
    @SerializedName("gastgeber_sterne") public Byte gastgeber_sterne;
    @SerializedName("gastgeber_kommentar") public String gastgeber_kommentar;
    @SerializedName("essen_sterne") public Byte essen_sterne;
    @SerializedName("essen_kommentar") public String essen_kommentar;
    @SerializedName("abend_sterne") public Byte abend_sterne;
    @SerializedName("abend_kommentar") public String abend_kommentar;
}
