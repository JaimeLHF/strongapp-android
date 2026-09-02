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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.databinding.FragmentWorkoutsBinding;
import br.com.strongapp.model.ApiMessage;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutProgress;
import br.com.strongapp.util.IsoWeek;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Lista os treinos do usuário com o percentual de conclusão da semana (RF06 e RF08). */
public class WorkoutsFragment extends Fragment implements WorkoutAdapter.Listener {

    private FragmentWorkoutsBinding binding;
    private WorkoutAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkoutsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new WorkoutAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.swipe.setOnRefreshListener(this::load);
        binding.addButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateWorkoutActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        binding.swipe.setRefreshing(true);
        ApiClient.api(requireContext()).workouts().enqueue(new Callback<List<Workout>>() {
            @Override
            public void onResponse(@NonNull Call<List<Workout>> call, @NonNull Response<List<Workout>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    loadProgress(response.body());
                } else {
                    binding.swipe.setRefreshing(false);
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Workout>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.swipe.setRefreshing(false);
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    /** Busca o progresso de todas as semanas e fica só com o da semana ISO atual. */
    private void loadProgress(List<Workout> workouts) {
        IsoWeek now = IsoWeek.current();
        ApiClient.api(requireContext()).allProgress().enqueue(new Callback<List<WorkoutProgress>>() {
            @Override
            public void onResponse(@NonNull Call<List<WorkoutProgress>> call,
                                   @NonNull Response<List<WorkoutProgress>> response) {
                if (binding == null) return;
                Map<String, Double> byWorkout = new HashMap<>();
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
                render(workouts, new HashMap<>());
            }
        });
    }

    private void render(List<Workout> workouts, Map<String, Double> progress) {
        binding.swipe.setRefreshing(false);
        adapter.submit(workouts, progress);
        binding.emptyState.setVisibility(workouts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOpen(Workout workout) {
        Intent intent = new Intent(requireContext(), WorkoutDetailActivity.class);
        intent.putExtra(WorkoutDetailActivity.EXTRA_ID, workout.id);
        intent.putExtra(WorkoutDetailActivity.EXTRA_TITLE, workout.title);
        startActivity(intent);
    }

    @Override
    public void onLongPress(Workout workout) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(workout.title)
                .setMessage("Excluir este treino?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> delete(workout))
                .show();
    }

    private void delete(Workout workout) {
        ApiClient.api(requireContext()).deleteWorkout(workout.id).enqueue(new Callback<ApiMessage>() {
            @Override
            public void onResponse(@NonNull Call<ApiMessage> call, @NonNull Response<ApiMessage> response) {
                if (binding == null) return;
                if (response.isSuccessful()) {
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

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
