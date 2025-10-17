package com.example.boardgamer;

import com.google.gson.annotations.SerializedName;
public class Nachricht {
    @SerializedName("id") public long id;
    @SerializedName("spieler_name") public String spieler_name;
    @SerializedName("nachricht") public String nachricht;

}
