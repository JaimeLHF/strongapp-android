package br.com.strongapp.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import br.com.strongapp.data.SessionManager;

/** Aplica o tema salvo. O protótipo do trabalho é escuro, então esse é o padrão. */
public final class ThemeMode {

    private ThemeMode() {
    }

    public static void apply(Context context) {
        set(SessionManager.get(context).isDarkMode());
    }

    /** Salva a escolha e troca o tema (as telas abertas são recriadas pelo AppCompat). */
    public static void toggle(Context context) {
        SessionManager session = SessionManager.get(context);
        boolean dark = !session.isDarkMode();
        session.setDarkMode(dark);
        set(dark);
    }

    private static void set(boolean dark) {
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
