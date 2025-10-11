package com.example.boardgamer;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Spieltermin {
    @SerializedName("id") public long id;
    @SerializedName("gastgeber_name") public String gastgeber_name;
    @SerializedName("termin") public String termin;
}
