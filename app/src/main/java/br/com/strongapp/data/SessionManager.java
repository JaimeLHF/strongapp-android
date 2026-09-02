package br.com.strongapp.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda o token Sanctum e o endereço da API em SharedPreferences. */
public class SessionManager {

    private static final String PREFS = "strongapp_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_BASE_URL = "base_url";

    private static SessionManager instance;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager get(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveSession(String token, String name, String email) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_NAME)
                .remove(KEY_EMAIL)
                .apply();
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, ApiConfig.DEFAULT_BASE_URL);
    }

    /** Normaliza para terminar em "/api/", que é o que o Retrofit espera. */
    public void setBaseUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) {
            prefs.edit().remove(KEY_BASE_URL).apply();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        if (!url.endsWith("/api/")) {
            url = url + "api/";
        }
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }
}
