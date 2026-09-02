package br.com.strongapp.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fábrica do cliente Retrofit. O endereço base é lido da sessão, então trocar o IP
 * do servidor na tela de login reconstrói o cliente sem reinstalar o app.
 */
public final class ApiClient {

    private static Retrofit retrofit;
    private static String builtFor;

    private ApiClient() {
    }

    public static synchronized StrongApi api(Context context) {
        SessionManager session = SessionManager.get(context);
        String baseUrl = session.getBaseUrl();

        if (retrofit == null || !baseUrl.equals(builtFor)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            Interceptor auth = chain -> {
                Request.Builder builder = chain.request().newBuilder()
                        .header("Accept", "application/json");
                String token = session.getToken();
                if (token != null) {
                    builder.header("Authorization", "Bearer " + token);
                }
                return chain.proceed(builder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(auth)
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(new Gson()))
                    .build();
            builtFor = baseUrl;
        }
        return retrofit.create(StrongApi.class);
    }

    /** Força a reconstrução do cliente (usado ao trocar o endereço do servidor). */
    public static synchronized void reset() {
        retrofit = null;
        builtFor = null;
    }

    /**
     * Traduz o corpo de erro do Laravel ({"message": ..., "errors": {...}}) para
     * uma frase legível ao usuário.
     */
    public static String errorMessage(Response<?> response) {
        if (response.code() == 401) {
            return "Sessão expirada. Entre novamente.";
        }
        try {
            if (response.errorBody() == null) {
                return "Erro " + response.code();
            }
            String raw = response.errorBody().string();
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) {
                return "Erro " + response.code();
            }
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("errors") && obj.get("errors").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("errors").entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value.isJsonArray() && value.getAsJsonArray().size() > 0) {
                        return value.getAsJsonArray().get(0).getAsString();
                    }
                }
            }
            if (obj.has("message")) {
                return obj.get("message").getAsString();
            }
            return "Erro " + response.code();
        } catch (IOException | RuntimeException e) {
            return "Erro " + response.code();
        }
    }

    public static String failureMessage(Throwable t) {
        return "Não foi possível falar com o servidor. Verifique o endereço e a rede.";
    }
}
