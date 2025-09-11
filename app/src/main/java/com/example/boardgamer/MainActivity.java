package com.example.boardgamer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);;

    }

    ArrayList<Spieleabend> anzeigenNächsteSpieleabende() {
        ArrayList<Spieleabend> spieleabendListe = new ArrayList<>();

        return spieleabendListe;
    }

    void befürwortenSpielFürSpieleabend(String spiel, Spieler spieler, Spieleabend spielabend) {
        spielabend.spielListe.putIfAbsent(spiel, new HashSet<>());
        spielabend.spielListe.get(spiel).add(spieler);
    }

    void absagenSpielFürSpieleabend(String spiel, Spieler spieler, Spieleabend spielabend) {
        spielabend.spielListe.get(spiel).remove(spieler);
    }

    void bewertenGastgeber(Spieler spieler, int sterne, String kommentar, Spieleabend spielabend) {
        Bewertung bewertung = new Bewertung();
        bewertung.sterne = sterne;
        bewertung.bewertung = kommentar;
        spielabend.gastgeberBewertung.put(spieler, bewertung);
    }

    void bewertenEssen(Spieler spieler, int sterne, String kommentar, Spieleabend spielabend) {
        Bewertung bewertung = new Bewertung();
        bewertung.sterne = sterne;
        bewertung.bewertung = kommentar;
        spielabend.essenBewertung.put(spieler, bewertung);
    }

    void bewertenAbend(Spieler spieler, int sterne, String kommentar, Spieleabend spielabend) {
        Bewertung bewertung = new Bewertung();
        bewertung.sterne = sterne;
        bewertung.bewertung = kommentar;
        spielabend.abendBewertung.put(spieler, bewertung);
    }

    void sendenSpielerNachricht(Spieler spieler, String nachricht) {
        spieler.nachrichten.add(nachricht);
    }

    ArrayList<String> anzeigenSpielerNachrichten(Spieler spieler) {
        return spieler.nachrichten;
    }
}