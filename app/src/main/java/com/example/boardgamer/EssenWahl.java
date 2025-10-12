package com.example.boardgamer;

import com.google.gson.annotations.SerializedName;

public class EssenWahl {
    @SerializedName("spieltermin_id") public long spieltermin_id;
    @SerializedName("spieler_name") public String spieler_name;
    @SerializedName("essensrichtung_id") public long essensrichtung_id;
}
