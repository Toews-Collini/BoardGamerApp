package com.example.boardgamer;

import com.google.gson.annotations.SerializedName;

public class SpieleabendView {
    @SerializedName("name") String name;
    @SerializedName("plz") String plz;
    @SerializedName("ort") String ort;
    @SerializedName("strasse") String strasse;
    @SerializedName("termin") String termin;
}
