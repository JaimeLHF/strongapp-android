package br.com.strongapp;

import android.app.Application;

import br.com.strongapp.util.ThemeMode;

/** Aplica o tema salvo antes de qualquer tela abrir. */
public class StrongApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeMode.apply(this);
    }
}
