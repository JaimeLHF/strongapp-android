package br.com.strongapp.ui;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.data.SessionManager;
import br.com.strongapp.databinding.ActivityWorkoutDetailBinding;
import br.com.strongapp.databinding.DialogReminderBinding;
import br.com.strongapp.model.CheckRequest;
import br.com.strongapp.model.ExerciseCheck;
import br.com.strongapp.model.ExerciseGroup;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutExercise;
import br.com.strongapp.model.ShareRequest;
import br.com.strongapp.model.WorkoutProgress;
import br.com.strongapp.model.WorkoutShare;
import br.com.strongapp.util.IsoWeek;
import br.com.strongapp.util.ProgressImage;
import br.com.strongapp.util.Reminders;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

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
            if (id == R.id.action_reminder) {
                showReminderDialog();
                return true;
            }
            if (id == R.id.action_share_progress) {
                shareProgressImage();
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
     * Escolha dos dias e do horário do lembrete deste treino (RF15).
     * Desmarcar todos os dias desliga o lembrete.
     */
    private void showReminderDialog() {
        DialogReminderBinding dialog = DialogReminderBinding.inflate(getLayoutInflater());
        CheckBox[] boxes = {dialog.day1, dialog.day2, dialog.day3, dialog.day4,
                dialog.day5, dialog.day6, dialog.day7};

        Set<Integer> saved = Reminders.days(this, workoutId);
        for (int i = 0; i < boxes.length; i++) {
            boxes[i].setChecked(saved.contains(i + 1));
        }

        final int[] time = {Reminders.hour(this, workoutId), Reminders.minute(this, workoutId)};
        dialog.timeButton.setText(getString(R.string.reminder_time, time[0], time[1]));
        dialog.timeButton.setOnClickListener(v ->
                new TimePickerDialog(this, (view, hour, minute) -> {
                    time[0] = hour;
                    time[1] = minute;
                    dialog.timeButton.setText(getString(R.string.reminder_time, hour, minute));
                }, time[0], time[1], true).show());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reminder)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (d, which) -> {
                    Set<Integer> days = new HashSet<>();
                    for (int i = 0; i < boxes.length; i++) {
                        if (boxes[i].isChecked()) {
                            days.add(i + 1);
                        }
                    }

                    String title = binding.toolbar.getTitle() == null
                            ? "" : binding.toolbar.getTitle().toString();
                    Reminders.save(this, workoutId, title, days, time[0], time[1]);

                    if (days.isEmpty()) {
                        toast(getString(R.string.reminder_cleared));
                    } else {
                        askNotificationPermission();
                        toast(getString(R.string.reminder_saved, Reminders.summary(this, workoutId)));
                    }
                })
                .show();
    }

    /** No Android 13+ a notificação só aparece com permissão do usuário. */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        toast(getString(R.string.reminder_permission_needed));
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
    }

    /** Gera a imagem 9:16 do progresso da semana e entrega pelo menu do aparelho (RF13). */
    private void shareProgressImage() {
        int total = exercises.size();
        int done = 0;
        for (WorkoutExercise item : exercises) {
            if (item.exerciseId != null && checkedIds.contains(item.exerciseId)) {
                done++;
            }
        }
        int percentage = total == 0 ? 0 : Math.round(done * 100f / total);
        String title = binding.toolbar.getTitle() == null ? "" : binding.toolbar.getTitle().toString();

        try {
            File file = ProgressImage.create(this, title, done, total, week.week());
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".exports", file);
            Intent intent = new Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_progress_text, percentage, title))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share_progress)));
        } catch (IOException e) {
            toast(getString(R.string.share_progress_failed));
        }
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
