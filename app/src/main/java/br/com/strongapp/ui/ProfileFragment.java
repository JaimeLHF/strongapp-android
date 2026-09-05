package br.com.strongapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import br.com.strongapp.data.ApiClient;
import br.com.strongapp.data.SessionManager;
import br.com.strongapp.databinding.FragmentProfileBinding;
import br.com.strongapp.model.ApiMessage;
import br.com.strongapp.model.ProfileStats;
import br.com.strongapp.model.ProfileUpdateRequest;
import br.com.strongapp.model.User;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Perfil com estatísticas reais e edição de nome (RF12). */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.saveButton.setOnClickListener(v -> save());
        binding.achievementsButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AchievementsActivity.class)));
        binding.exportButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExportActivity.class)));
        binding.logoutButton.setOnClickListener(v -> logout());

        loadProfile();
        loadStats();
    }

    private void loadProfile() {
        ApiClient.api(requireContext()).profile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    binding.firstNameInput.setText(user.firstName == null ? "" : user.firstName);
                    binding.lastNameInput.setText(user.lastName == null ? "" : user.lastName);
                    binding.emailLabel.setText(user.email == null ? "" : user.email);

                    String full = ((user.firstName == null ? "" : user.firstName) + " "
                            + (user.lastName == null ? "" : user.lastName)).trim();
                    binding.nameLabel.setText(full.isEmpty() ? user.email : full);
                    binding.initialsLabel.setText(initials(full, user.email));
                } else {
                    Toast.makeText(requireContext(), ApiClient.errorMessage(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (binding == null) return;
                Toast.makeText(requireContext(), ApiClient.failureMessage(t), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadStats() {
        ApiClient.api(requireContext()).profileStats().enqueue(new Callback<ProfileStats>() {
            @Override
            public void onResponse(@NonNull Call<ProfileStats> call, @NonNull Response<ProfileStats> response) {
                if (binding == null || !response.isSuccessful() || response.body() == null) return;
                ProfileStats stats = response.body();
                binding.statWorkouts.setText(String.valueOf(stats.totalWorkouts));
                binding.statExercises.setText(String.valueOf(stats.uniqueExercises));
                binding.statCompletion.setText(
                        String.format(Locale.getDefault(), "%.0f%%", stats.avgCompletion));
                binding.statStreak.setText(String.valueOf(stats.currentStreak));
            }

            @Override
            public void onFailure(@NonNull Call<ProfileStats> call, @NonNull Throwable t) {
                // Estatística é informação secundária: falhar aqui não bloqueia a tela.
            }
        });
    }

    private void save() {
        String firstName = text(binding.firstNameInput.getText());
        String lastName = text(binding.lastNameInput.getText());

        binding.saveButton.setEnabled(false);
        ApiClient.api(requireContext())
                .updateProfile(new ProfileUpdateRequest(firstName, lastName))
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                        if (binding == null) return;
                        binding.saveButton.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Perfil atualizado.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), ApiClient.errorMessage(response), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                        if (binding == null) return;
                        binding.saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), ApiClient.failureMessage(t), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logout() {
        ApiClient.api(requireContext()).logout().enqueue(new Callback<ApiMessage>() {
            @Override
            public void onResponse(@NonNull Call<ApiMessage> call, @NonNull Response<ApiMessage> response) {
                finishLogout();
            }

            @Override
            public void onFailure(@NonNull Call<ApiMessage> call, @NonNull Throwable t) {
                // Mesmo sem resposta do servidor, a sessão local é encerrada.
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        if (!isAdded()) return;
        SessionManager.get(requireContext()).clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    /** Iniciais do nome; sem nome, a primeira letra do e-mail. */
    private static String initials(String fullName, String email) {
        StringBuilder builder = new StringBuilder();
        for (String part : fullName.split("\\s+")) {
            if (!part.isEmpty() && builder.length() < 2) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        if (builder.length() == 0 && email != null && !email.isEmpty()) {
            builder.append(Character.toUpperCase(email.charAt(0)));
        }
        return builder.toString();
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
