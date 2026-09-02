package br.com.strongapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.data.SessionManager;
import br.com.strongapp.databinding.ActivityLoginBinding;
import br.com.strongapp.databinding.DialogServerBinding;
import br.com.strongapp.model.AuthResponse;
import br.com.strongapp.model.LoginRequest;
import br.com.strongapp.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Entrada do app: login e cadastro (RF01 e RF02). */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager session;
    private boolean registerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = SessionManager.get(this);

        if (session.isLoggedIn()) {
            goToMain();
            return;
        }

        binding.toggleButton.setOnClickListener(v -> setRegisterMode(!registerMode));
        binding.submitButton.setOnClickListener(v -> submit());
        binding.serverButton.setOnClickListener(v -> showServerDialog());
    }

    private void setRegisterMode(boolean enabled) {
        registerMode = enabled;
        int visibility = enabled ? View.VISIBLE : View.GONE;
        binding.firstNameLayout.setVisibility(visibility);
        binding.lastNameLayout.setVisibility(visibility);
        binding.formTitle.setText(enabled ? R.string.register_title : R.string.login_title);
        binding.submitButton.setText(enabled ? R.string.create_account : R.string.enter);
        binding.toggleButton.setText(enabled ? R.string.have_account : R.string.no_account);
    }

    private void submit() {
        String email = text(binding.emailInput.getText());
        String password = text(binding.passwordInput.getText());

        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError("Informe o e-mail");
            return;
        }
        if (password.length() < 6) {
            binding.passwordLayout.setError("A senha precisa de ao menos 6 caracteres");
            return;
        }

        setLoading(true);
        Callback<AuthResponse> callback = new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().token != null) {
                    AuthResponse body = response.body();
                    session.saveSession(body.token,
                            body.user != null ? body.user.displayName() : "",
                            body.user != null ? body.user.email : "");
                    goToMain();
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoading(false);
                toast(ApiClient.failureMessage(t));
            }
        };

        if (registerMode) {
            RegisterRequest body = new RegisterRequest(
                    text(binding.firstNameInput.getText()),
                    text(binding.lastNameInput.getText()),
                    email, password);
            ApiClient.api(this).register(body).enqueue(callback);
        } else {
            ApiClient.api(this).login(new LoginRequest(email, password)).enqueue(callback);
        }
    }

    private void showServerDialog() {
        DialogServerBinding dialogBinding = DialogServerBinding.inflate(getLayoutInflater());
        dialogBinding.serverInput.setText(session.getBaseUrl());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.server_settings)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    session.setBaseUrl(text(dialogBinding.serverInput.getText()));
                    ApiClient.reset();
                    toast("Servidor: " + session.getBaseUrl());
                })
                .show();
    }

    private void setLoading(boolean loading) {
        binding.loading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setEnabled(!loading);
        binding.toggleButton.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
