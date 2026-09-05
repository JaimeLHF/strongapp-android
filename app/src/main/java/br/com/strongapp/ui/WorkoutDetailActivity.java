package br.com.strongapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.data.SessionManager;
import br.com.strongapp.databinding.ActivityWorkoutDetailBinding;
import br.com.strongapp.model.CheckRequest;
import br.com.strongapp.model.ExerciseCheck;
import br.com.strongapp.model.ExerciseGroup;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutExercise;
import br.com.strongapp.model.ShareRequest;
import br.com.strongapp.model.WorkoutProgress;
import br.com.strongapp.model.WorkoutShare;
import br.com.strongapp.util.IsoWeek;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Execução do treino: lista de exercícios, marcação de concluído por semana ISO
 * e cronômetro de descanso (RF07, RF08 e RF09).
 */
public class WorkoutDetailActivity extends AppCompatActivity implements WorkoutExerciseAdapter.Listener {

    public static final String EXTRA_ID = "workout_id";
    public static final String EXTRA_TITLE = "workout_title";

    private ActivityWorkoutDetailBinding binding;
    private WorkoutExerciseAdapter adapter;
    private String workoutId;
    private final IsoWeek week = IsoWeek.current();

    private List<WorkoutExercise> exercises = new ArrayList<>();
    private Map<String, String> groupNames = new HashMap<>();
    private Set<String> checkedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkoutDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        workoutId = getIntent().getStringExtra(EXTRA_ID);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        binding.toolbar.setTitle(title == null ? "" : title);
        binding.toolbar.inflateMenu(R.menu.workout_detail);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_diary) {
                Intent diary = new Intent(this, DiaryActivity.class);
                diary.putExtra(DiaryActivity.EXTRA_ID, workoutId);
                diary.putExtra(DiaryActivity.EXTRA_TITLE, binding.toolbar.getTitle());
                startActivity(diary);
                return true;
            }
            if (id == R.id.action_share) {
                shareWorkout();
                return true;
            }
            if (id == R.id.action_edit) {
                Intent edit = new Intent(this, CreateWorkoutActivity.class);
                edit.putExtra(CreateWorkoutActivity.EXTRA_ID, workoutId);
                startActivity(edit);
                return true;
            }
            return false;
        });
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new WorkoutExerciseAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.swipe.setOnRefreshListener(this::load);
        binding.timerButton.setOnClickListener(v ->
                new TimerBottomSheet().show(getSupportFragmentManager(), "timer"));

        load();
    }

    private void load() {
        if (workoutId == null) {
            finish();
            return;
        }
        binding.swipe.setRefreshing(true);
        ApiClient.api(this).workout(workoutId).enqueue(new Callback<Workout>() {
            @Override
            public void onResponse(@NonNull Call<Workout> call, @NonNull Response<Workout> response) {
                if (response.isSuccessful() && response.body() != null) {
                    unpack(response.body());
                    loadChecks();
                } else {
                    binding.swipe.setRefreshing(false);
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Workout> call, @NonNull Throwable t) {
                binding.swipe.setRefreshing(false);
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    /** A API devolve os exercícios soltos e os agrupados; aqui viram uma lista só, ordenada. */
    private void unpack(Workout workout) {
        exercises = workout.workoutExercises == null
                ? new ArrayList<>() : new ArrayList<>(workout.workoutExercises);

        groupNames = new HashMap<>();
        if (workout.exerciseGroups != null) {
            for (ExerciseGroup group : workout.exerciseGroups) {
                if (group.id != null) {
                    groupNames.put(group.id, group.name);
                }
            }
        }

        Collections.sort(exercises, (a, b) -> {
            int left = a.orderIndex == null ? 0 : a.orderIndex;
            int right = b.orderIndex == null ? 0 : b.orderIndex;
            return Integer.compare(left, right);
        });
    }

    private void loadChecks() {
        ApiClient.api(this).checks(workoutId, week.year(), week.week())
                .enqueue(new Callback<List<ExerciseCheck>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ExerciseCheck>> call,
                                           @NonNull Response<List<ExerciseCheck>> response) {
                        binding.swipe.setRefreshing(false);
                        checkedIds = new HashSet<>();
                        if (response.isSuccessful() && response.body() != null) {
                            for (ExerciseCheck check : response.body()) {
                                if (check.checked && check.exerciseId != null) {
                                    checkedIds.add(check.exerciseId);
                                }
                            }
                        }
                        adapter.submit(exercises, groupNames, checkedIds);
                        renderProgress();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ExerciseCheck>> call, @NonNull Throwable t) {
                        binding.swipe.setRefreshing(false);
                        adapter.submit(exercises, groupNames, new HashSet<>());
                        toast(ApiClient.failureMessage(t));
                    }
                });
    }

    /** Espelha localmente a mesma conta do backend, para o número reagir na hora. */
    private void renderProgress() {
        int total = exercises.size();
        int done = 0;
        for (WorkoutExercise item : exercises) {
            if (item.exerciseId != null && checkedIds.contains(item.exerciseId)) {
                done++;
            }
        }
        int percentage = total == 0 ? 0 : Math.round(done * 100f / total);
        binding.progressBar.setProgress(percentage);
        binding.progressLabel.setText(String.format(Locale.getDefault(),
                "%d de %d nesta semana · %d%%", done, total, percentage));
    }

    @Override
    public void onToggle(WorkoutExercise item, boolean checked) {
        if (item.exerciseId == null) return;

        adapter.setChecked(item.exerciseId, checked);
        if (checked) {
            checkedIds.add(item.exerciseId);
        } else {
            checkedIds.remove(item.exerciseId);
        }
        renderProgress();

        CheckRequest body = new CheckRequest(item.exerciseId, week.year(), week.week(), checked);
        ApiClient.api(this).toggleCheck(workoutId, body).enqueue(new Callback<ExerciseCheck>() {
            @Override
            public void onResponse(@NonNull Call<ExerciseCheck> call, @NonNull Response<ExerciseCheck> response) {
                if (response.isSuccessful()) {
                    persistProgress();
                } else {
                    revert(item, checked, ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ExerciseCheck> call, @NonNull Throwable t) {
                revert(item, checked, ApiClient.failureMessage(t));
            }
        });
    }

    /** Desfaz a marcação otimista quando o servidor recusa. */
    private void revert(WorkoutExercise item, boolean attempted, String message) {
        if (attempted) {
            checkedIds.remove(item.exerciseId);
        } else {
            checkedIds.add(item.exerciseId);
        }
        adapter.submit(exercises, groupNames, checkedIds);
        renderProgress();
        toast(message);
    }

    /** Pede ao backend que recalcule e grave o progresso da semana. */
    private void persistProgress() {
        ApiClient.api(this).calculateProgress(workoutId, week.year(), week.week())
                .enqueue(new Callback<WorkoutProgress>() {
                    @Override
                    public void onResponse(@NonNull Call<WorkoutProgress> call,
                                           @NonNull Response<WorkoutProgress> response) {
                        // O número já foi atualizado na tela; esta chamada só persiste.
                    }

                    @Override
                    public void onFailure(@NonNull Call<WorkoutProgress> call, @NonNull Throwable t) {
                        // Sem rede o progresso é recalculado na próxima abertura da tela.
                    }
                });
    }

    /**
     * Gera o link público do treino (POST /workout-shares) e entrega pelo
     * compartilhamento do Android — o mesmo /share/{token} que a versão web abre.
     */
    private void shareWorkout() {
        String title = binding.toolbar.getTitle() == null ? "" : binding.toolbar.getTitle().toString();
        ApiClient.api(this).shareWorkout(new ShareRequest(workoutId, title))
                .enqueue(new Callback<WorkoutShare>() {
                    @Override
                    public void onResponse(@NonNull Call<WorkoutShare> call, @NonNull Response<WorkoutShare> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().shareToken == null) {
                            toast(getString(R.string.share_failed));
                            return;
                        }
                        String link = SessionManager.get(WorkoutDetailActivity.this).getShareUrl()
                                + response.body().shareToken;
                        Intent intent = new Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, title, link));
                        startActivity(Intent.createChooser(intent, getString(R.string.share_workout)));
                    }

                    @Override
                    public void onFailure(@NonNull Call<WorkoutShare> call, @NonNull Throwable t) {
                        toast(ApiClient.failureMessage(t));
                    }
                });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
