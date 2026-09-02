package br.com.strongapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.databinding.ItemWorkoutExerciseBinding;
import br.com.strongapp.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WorkoutExerciseAdapter extends RecyclerView.Adapter<WorkoutExerciseAdapter.ViewHolder> {

    public interface Listener {
        void onToggle(WorkoutExercise item, boolean checked);
    }

    private final List<WorkoutExercise> items = new ArrayList<>();
    private final Map<String, String> groupNames = new HashMap<>();
    private final Set<String> checkedExerciseIds = new HashSet<>();
    private final Listener listener;

    public WorkoutExerciseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WorkoutExercise> exercises, Map<String, String> groups, Set<String> checked) {
        items.clear();
        if (exercises != null) {
            items.addAll(exercises);
        }
        groupNames.clear();
        if (groups != null) {
            groupNames.putAll(groups);
        }
        checkedExerciseIds.clear();
        if (checked != null) {
            checkedExerciseIds.addAll(checked);
        }
        notifyDataSetChanged();
    }

    public void setChecked(String exerciseId, boolean checked) {
        if (checked) {
            checkedExerciseIds.add(exerciseId);
        } else {
            checkedExerciseIds.remove(exerciseId);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemWorkoutExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemWorkoutExerciseBinding binding;

        ViewHolder(ItemWorkoutExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WorkoutExercise item, int position) {
            binding.position.setText(String.valueOf(position));
            binding.name.setText(item.exercise != null ? item.exercise.name : "Exercício");

            String groupName = item.groupId == null ? null : groupNames.get(item.groupId);
            String muscle = item.exercise == null ? null : item.exercise.muscleGroup;
            String subtitle;
            if (groupName != null) {
                subtitle = "Superset: " + groupName;
            } else {
                subtitle = muscle == null ? "" : muscle;
            }
            binding.groupLabel.setText(subtitle);
            binding.groupLabel.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);

            binding.sets.setText(item.sets == null ? "-" : String.valueOf(item.sets));
            binding.reps.setText(item.reps == null ? "-" : String.valueOf(item.reps));
            binding.weight.setText(item.weight == null
                    ? "-" : String.format(Locale.getDefault(), "%.0f", item.weight));
            binding.rest.setText(item.restTime == null
                    ? "-" : String.format(Locale.getDefault(), "%ds", item.restTime));

            boolean hasInstructions = item.exercise != null
                    && item.exercise.instructions != null
                    && !item.exercise.instructions.isEmpty();
            binding.instructions.setText(hasInstructions ? item.exercise.instructions : "");
            binding.instructions.setVisibility(View.GONE);
            binding.getRoot().setOnClickListener(v -> {
                if (!hasInstructions) return;
                boolean visible = binding.instructions.getVisibility() == View.VISIBLE;
                binding.instructions.setVisibility(visible ? View.GONE : View.VISIBLE);
            });

            binding.check.setOnCheckedChangeListener(null);
            binding.check.setChecked(item.exerciseId != null && checkedExerciseIds.contains(item.exerciseId));
            binding.check.setOnCheckedChangeListener((buttonView, isChecked) ->
                    listener.onToggle(item, isChecked));
        }
    }
}
