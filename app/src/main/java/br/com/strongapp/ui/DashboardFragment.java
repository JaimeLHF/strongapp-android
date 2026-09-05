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

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.data.SessionManager;
import br.com.strongapp.databinding.FragmentDashboardBinding;
import br.com.strongapp.databinding.ItemStatCardBinding;
import br.com.strongapp.model.ProfileStats;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutProgress;
import br.com.strongapp.util.IsoWeek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela inicial do protótipo: saudação, atalhos e o bloco "Seus Números"
 * (treinos criados, exercícios únicos e progresso da semana).
 */
public class DashboardFragment extends Fragment {

    /** Mesmos períodos do seletor da versão web, convertidos para semanas ISO. */
    private static final String[] PERIODS = {"7 dias", "30 dias", "90 dias", "1 ano"};
    private static final int[] PERIOD_WEEKS = {1, 5, 13, 52};

    private FragmentDashboardBinding binding;
    private List<WorkoutProgress> progressCache = new ArrayList<>();
    private int periodIndex = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        card(binding.cardWorkouts, R.string.stat_created_workouts,
                R.string.stat_created_workouts_caption, R.drawable.ic_calendar);
        card(binding.cardExercises, R.string.stat_unique_exercises,
                R.string.stat_unique_exercises_caption, R.drawable.ic_pulse);
        card(binding.cardWeekly, R.string.stat_weekly,
                R.string.stat_weekly_caption, R.drawable.ic_trending);
        binding.cardWeekly.statValue.setText("0%");

        String name = SessionManager.get(requireContext()).getName();
        if (!name.isEmpty()) {
            String firstName = name.split(" ")[0];
            binding.greetingLabel.setText(getString(R.string.dashboard_greeting_named, firstName));
        }

        binding.periodInput.setSimpleItems(PERIODS);
        binding.periodInput.setText(PERIODS[periodIndex], false);
        binding.periodInput.setOnItemClickListener((parent, view1, position, id) -> {
            periodIndex = position;
            renderChart();
        });

        binding.swipe.setOnRefreshListener(this::load);
        binding.createWorkoutButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateWorkoutActivity.class)));
        binding.rankingButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RankingActivity.class)));
        binding.seeExercisesButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_exercises);
            }
        });
    }

    private void card(ItemStatCardBinding card, int label, int caption, int icon) {
        card.statLabel.setText(label);
        card.statCaption.setText(caption);
        card.statIcon.setImageResource(icon);
        card.statValue.setText("0");
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        binding.swipe.setRefreshing(true);
        loadStats();
        loadWeeklyProgress();
    }

    private void loadStats() {
        ApiClient.api(requireContext()).profileStats().enqueue(new Callback<ProfileStats>() {
            @Override
            public void onResponse(@NonNull Call<ProfileStats> call, @NonNull Response<ProfileStats> response) {
                if (binding == null || !response.isSuccessful() || response.body() == null) return;
                ProfileStats stats = response.body();
                binding.cardWorkouts.statValue.setText(String.valueOf(stats.totalWorkouts));
                binding.cardExercises.statValue.setText(String.valueOf(stats.uniqueExercises));
            }

            @Override
            public void onFailure(@NonNull Call<ProfileStats> call, @NonNull Throwable t) {
                if (binding == null) return;
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private void loadWeeklyProgress() {
        ApiClient.api(requireContext()).workouts().enqueue(new Callback<List<Workout>>() {
            @Override
            public void onResponse(@NonNull Call<List<Workout>> call, @NonNull Response<List<Workout>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    loadProgress(response.body());
                } else {
                    binding.swipe.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Workout>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.swipe.setRefreshing(false);
            }
        });
    }

    /**
     * O progresso semanal é a média da conclusão da semana ISO atual sobre todos
     * os treinos — um treino sem registro na semana conta como 0%.
     */
    private void loadProgress(List<Workout> workouts) {
        IsoWeek now = IsoWeek.current();
        ApiClient.api(requireContext()).allProgress().enqueue(new Callback<List<WorkoutProgress>>() {
            @Override
            public void onResponse(@NonNull Call<List<WorkoutProgress>> call,
                                   @NonNull Response<List<WorkoutProgress>> response) {
                if (binding == null) return;
                Map<String, Double> byWorkout = new HashMap<>();
                progressCache = response.isSuccessful() && response.body() != null
                        ? response.body() : new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (WorkoutProgress progress : response.body()) {
                        boolean sameWeek = progress.year != null && progress.week != null
                                && progress.year == now.year() && progress.week == now.week();
                        if (sameWeek && progress.workoutId != null && progress.completionPercentage != null) {
                            byWorkout.put(progress.workoutId, progress.completionPercentage);
                        }
                    }
                }
                render(workouts, byWorkout);
            }

            @Override
            public void onFailure(@NonNull Call<List<WorkoutProgress>> call, @NonNull Throwable t) {
                if (binding == null) return;
                progressCache = new ArrayList<>();
                render(workouts, new HashMap<>());
            }
        });
    }

    private void render(List<Workout> workouts, Map<String, Double> progress) {
        binding.swipe.setRefreshing(false);

        double sum = 0;
        for (Workout workout : workouts) {
            Double value = progress.get(workout.id);
            sum += value == null ? 0 : value;
        }
        double weekly = workouts.isEmpty() ? 0 : sum / workouts.size();
        binding.cardWeekly.statValue.setText(String.format(Locale.getDefault(), "%.0f%%", weekly));

        renderChart();
    }

    /**
     * Série do gráfico: uma semana ISO por ponto, com a média de conclusão daquela
     * semana entre os treinos que tiveram registro. Fora do período escolhido, nada entra.
     */
    private void renderChart() {
        if (binding == null) return;

        IsoWeek now = IsoWeek.current();
        int weeks = PERIOD_WEEKS[periodIndex];

        // Chave "ano*100+semana" ordena naturalmente e evita comparar strings.
        Map<Integer, double[]> byWeek = new TreeMap<>();
        for (WorkoutProgress progress : progressCache) {
            if (progress.year == null || progress.week == null || progress.completionPercentage == null) {
                continue;
            }
            int distance = (now.year() - progress.year) * 52 + (now.week() - progress.week);
            if (distance < 0 || distance >= weeks) continue;

            int key = progress.year * 100 + progress.week;
            double[] acc = byWeek.get(key);
            if (acc == null) {
                acc = new double[2];
                byWeek.put(key, acc);
            }
            acc[0] += progress.completionPercentage;
            acc[1]++;
        }

        List<LineChartView.Point> points = new ArrayList<>();
        for (Map.Entry<Integer, double[]> entry : byWeek.entrySet()) {
            double[] acc = entry.getValue();
            points.add(new LineChartView.Point(
                    "S" + (entry.getKey() % 100), (float) (acc[0] / acc[1])));
        }

        boolean empty = points.isEmpty();
        binding.chart.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.chartCaption.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.chartEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) {
            binding.chartCaption.setText(getString(R.string.progress_chart_caption, points.size()));
            binding.chart.setPoints(points);
        }
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
