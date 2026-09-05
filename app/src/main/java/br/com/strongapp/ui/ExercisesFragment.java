package br.com.strongapp.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.databinding.DialogExerciseBinding;
import br.com.strongapp.databinding.FragmentExercisesBinding;
import br.com.strongapp.model.ApiMessage;
import br.com.strongapp.model.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Catálogo de exercícios com busca e filtro por grupo muscular (RF03 e RF05) e
 * gestão do catálogo — criar, editar e excluir (RF04). O toque longo em um
 * exercício abre editar/excluir; o botão flutuante cria um novo.
 */
public class ExercisesFragment extends Fragment implements ExerciseAdapter.Listener {

    private FragmentExercisesBinding binding;
    private ExerciseAdapter adapter;
    private final List<Exercise> all = new ArrayList<>();
    private String selectedGroup = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExercisesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ExerciseAdapter();
        adapter.setListener(this);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.addButton.setOnClickListener(v -> showEditor(null));

        binding.swipe.setOnRefreshListener(this::load);
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        load();
    }

    private void load() {
        binding.swipe.setRefreshing(true);
        ApiClient.api(requireContext()).exercises().enqueue(new Callback<List<Exercise>>() {
            @Override
            public void onResponse(@NonNull Call<List<Exercise>> call, @NonNull Response<List<Exercise>> response) {
                if (binding == null) return;
                binding.swipe.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    all.clear();
                    all.addAll(response.body());
                    buildChips();
                    applyFilters();
                } else {
                    Toast.makeText(requireContext(), ApiClient.errorMessage(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Exercise>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.swipe.setRefreshing(false);
                Toast.makeText(requireContext(), ApiClient.failureMessage(t), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void buildChips() {
        binding.groupChips.removeAllViews();

        TreeSet<String> groups = new TreeSet<>();
        for (Exercise exercise : all) {
            if (exercise.muscleGroup != null && !exercise.muscleGroup.isEmpty()) {
                groups.add(exercise.muscleGroup);
            }
        }

        addChip(getString(R.string.all_groups), null);
        for (String group : groups) {
            addChip(group, group);
        }
    }

    private void addChip(String label, String value) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(value == null && selectedGroup == null
                || value != null && value.equals(selectedGroup));
        chip.setOnClickListener(v -> {
            selectedGroup = value;
            applyFilters();
        });
        binding.groupChips.addView(chip);
    }

    private void applyFilters() {
        String query = binding.searchInput.getText() == null
                ? "" : binding.searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());

        List<Exercise> filtered = new ArrayList<>();
        for (Exercise exercise : all) {
            boolean matchesGroup = selectedGroup == null || selectedGroup.equals(exercise.muscleGroup);
            boolean matchesQuery = query.isEmpty()
                    || (exercise.name != null && exercise.name.toLowerCase(Locale.getDefault()).contains(query))
                    || (exercise.equipment != null && exercise.equipment.toLowerCase(Locale.getDefault()).contains(query));
            if (matchesGroup && matchesQuery) {
                filtered.add(exercise);
            }
        }

        adapter.submit(filtered);
        binding.emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** {@code exercise} nulo cria; preenchido edita. */
    private void showEditor(@Nullable Exercise exercise) {
        DialogExerciseBinding dialog = DialogExerciseBinding.inflate(getLayoutInflater());
        if (exercise != null) {
            dialog.nameInput.setText(exercise.name);
            dialog.muscleGroupInput.setText(exercise.muscleGroup);
            dialog.equipmentInput.setText(exercise.equipment);
            dialog.instructionsInput.setText(exercise.instructions);
            dialog.videoInput.setText(exercise.videoUrl);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(exercise == null ? R.string.new_exercise : R.string.edit_exercise)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (d, which) -> {
                    Exercise body = new Exercise();
                    body.name = text(dialog.nameInput.getText());
                    if (body.name.isEmpty()) {
                        toast(getString(R.string.exercise_name_required));
                        return;
                    }
                    body.muscleGroup = emptyToNull(text(dialog.muscleGroupInput.getText()));
                    body.equipment = emptyToNull(text(dialog.equipmentInput.getText()));
                    body.instructions = emptyToNull(text(dialog.instructionsInput.getText()));
                    body.videoUrl = emptyToNull(text(dialog.videoInput.getText()));
                    save(exercise, body);
                })
                .show();
    }

    private void save(@Nullable Exercise existing, Exercise body) {
        Call<Exercise> call = existing == null
                ? ApiClient.api(requireContext()).createExercise(body)
                : ApiClient.api(requireContext()).updateExercise(existing.id, body);

        call.enqueue(new Callback<Exercise>() {
            @Override
            public void onResponse(@NonNull Call<Exercise> call, @NonNull Response<Exercise> response) {
                if (binding == null) return;
                if (response.isSuccessful()) {
                    load();
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Exercise> call, @NonNull Throwable t) {
                if (binding == null) return;
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    @Override
    public void onExerciseLongPress(Exercise exercise) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(exercise.name)
                .setItems(new String[]{getString(R.string.edit), getString(R.string.delete)},
                        (dialog, which) -> {
                            if (which == 0) {
                                showEditor(exercise);
                            } else {
                                confirmDelete(exercise);
                            }
                        })
                .show();
    }

    private void confirmDelete(Exercise exercise) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(exercise.name)
                .setMessage(R.string.delete_exercise_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> delete(exercise))
                .show();
    }

    private void delete(Exercise exercise) {
        ApiClient.api(requireContext()).deleteExercise(exercise.id).enqueue(new Callback<ApiMessage>() {
            @Override
            public void onResponse(@NonNull Call<ApiMessage> call, @NonNull Response<ApiMessage> response) {
                if (binding == null) return;
                if (response.isSuccessful()) {
                    toast(getString(R.string.exercise_deleted));
                    load();
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiMessage> call, @NonNull Throwable t) {
                if (binding == null) return;
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
