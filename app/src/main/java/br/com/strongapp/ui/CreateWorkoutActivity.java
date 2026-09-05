package br.com.strongapp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.databinding.ActivityCreateWorkoutBinding;
import br.com.strongapp.model.CreateWorkoutRequest;
import br.com.strongapp.model.Exercise;
import br.com.strongapp.model.ExerciseInput;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Montagem de um treino com séries, repetições, carga e descanso (RF06).
 * Com {@link #EXTRA_ID} no intent a mesma tela edita um treino existente.
 */
public class CreateWorkoutActivity extends AppCompatActivity implements PickedExerciseAdapter.Listener {

    /** Id do treino a editar. Ausente significa criar um treino novo. */
    public static final String EXTRA_ID = "workout_id";

    private static final String[] DIFFICULTIES = {"Iniciante", "Intermediário", "Avançado"};

    private ActivityCreateWorkoutBinding binding;
    private PickedExerciseAdapter adapter;
    private String editingId;

    private final List<PickedExercise> picked = new ArrayList<>();
    private final List<Exercise> catalog = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateWorkoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editingId = getIntent().getStringExtra(EXTRA_ID);
        if (editingId != null) {
            binding.toolbar.setTitle(R.string.edit_workout);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.difficultyInput.setSimpleItems(DIFFICULTIES);

        adapter = new PickedExerciseAdapter(picked, this);
        binding.pickedList.setLayoutManager(new LinearLayoutManager(this));
        binding.pickedList.setAdapter(adapter);

        binding.addExerciseButton.setOnClickListener(v -> showPicker());
        binding.saveButton.setOnClickListener(v -> save());

        updatePickedLabel();
        loadCatalog();

        if (editingId != null) {
            loadWorkout();
        }
    }

    /** Preenche o formulário com o treino salvo, para edição. */
    private void loadWorkout() {
        ApiClient.api(this).workout(editingId).enqueue(new Callback<Workout>() {
            @Override
            public void onResponse(@NonNull Call<Workout> call, @NonNull Response<Workout> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    toast(ApiClient.errorMessage(response));
                    return;
                }
                Workout workout = response.body();
                binding.titleInput.setText(workout.title);
                binding.descriptionInput.setText(workout.description);
                binding.durationInput.setText(workout.duration == null ? "" : String.valueOf(workout.duration));
                binding.difficultyInput.setText(workout.difficulty == null ? "" : workout.difficulty, false);

                picked.clear();
                if (workout.workoutExercises != null) {
                    for (WorkoutExercise item : workout.workoutExercises) {
                        if (item.exercise == null) continue;
                        PickedExercise entry = new PickedExercise(item.exercise);
                        entry.sets = item.sets;
                        entry.reps = item.reps;
                        entry.weight = item.weight;
                        entry.restTime = item.restTime;
                        picked.add(entry);
                    }
                }
                adapter.notifyDataSetChanged();
                updatePickedLabel();
            }

            @Override
            public void onFailure(@NonNull Call<Workout> call, @NonNull Throwable t) {
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private void loadCatalog() {
        ApiClient.api(this).exercises().enqueue(new Callback<List<Exercise>>() {
            @Override
            public void onResponse(@NonNull Call<List<Exercise>> call, @NonNull Response<List<Exercise>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    catalog.clear();
                    catalog.addAll(response.body());
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Exercise>> call, @NonNull Throwable t) {
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private void showPicker() {
        if (catalog.isEmpty()) {
            toast("Catálogo ainda não carregou.");
            loadCatalog();
            return;
        }

        String[] names = new String[catalog.size()];
        for (int i = 0; i < catalog.size(); i++) {
            Exercise exercise = catalog.get(i);
            names[i] = exercise.muscleGroup == null || exercise.muscleGroup.isEmpty()
                    ? exercise.name
                    : exercise.name + " (" + exercise.muscleGroup + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pick_exercise)
                .setItems(names, (dialog, which) -> {
                    picked.add(new PickedExercise(catalog.get(which)));
                    adapter.notifyItemInserted(picked.size() - 1);
                    updatePickedLabel();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onRemove(int position) {
        picked.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, picked.size() - position);
        updatePickedLabel();
    }

    private void updatePickedLabel() {
        binding.pickedLabel.setText(picked.size() == 1
                ? "1 exercício no treino"
                : picked.size() + " exercícios no treino");
    }

    private void save() {
        String title = text(binding.titleInput.getText());
        if (TextUtils.isEmpty(title)) {
            binding.titleInput.setError("Informe o título");
            return;
        }
        if (picked.isEmpty()) {
            toast("Adicione ao menos um exercício.");
            return;
        }

        CreateWorkoutRequest body = new CreateWorkoutRequest();
        body.title = title;
        body.description = emptyToNull(text(binding.descriptionInput.getText()));
        body.duration = parseInt(text(binding.durationInput.getText()));
        body.difficulty = emptyToNull(text(binding.difficultyInput.getText()));
        body.exercises = new ArrayList<>();

        for (int i = 0; i < picked.size(); i++) {
            PickedExercise item = picked.get(i);
            ExerciseInput input = new ExerciseInput();
            input.exerciseId = item.exercise.id;
            input.sets = item.sets;
            input.reps = item.reps;
            input.weight = item.weight;
            input.restTime = item.restTime;
            input.orderIndex = i;
            body.exercises.add(input);
        }

        binding.saveButton.setEnabled(false);
        Call<Workout> call = editingId == null
                ? ApiClient.api(this).createWorkout(body)
                : ApiClient.api(this).updateWorkout(editingId, body);

        call.enqueue(new Callback<Workout>() {
            @Override
            public void onResponse(@NonNull Call<Workout> call, @NonNull Response<Workout> response) {
                binding.saveButton.setEnabled(true);
                if (response.isSuccessful()) {
                    toast(getString(editingId == null ? R.string.workout_created : R.string.workout_updated));
                    finish();
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Workout> call, @NonNull Throwable t) {
                binding.saveButton.setEnabled(true);
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private static Integer parseInt(String value) {
        try {
            return value.isEmpty() ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
