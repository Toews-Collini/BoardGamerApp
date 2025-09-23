package com.example.boardgamer;

import java.util.ArrayList;

public class Spieler {
    String name;
    Adresse adresse;
    Email emailAdresse;
    ArrayList<String> nachrichten = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
