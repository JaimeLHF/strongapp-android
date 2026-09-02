package br.com.strongapp.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.databinding.ItemWorkoutBinding;
import br.com.strongapp.model.Workout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(Workout workout);

        void onLongPress(Workout workout);
    }

    private final List<Workout> items = new ArrayList<>();
    private final Map<String, Double> progressByWorkout = new HashMap<>();
    private final Listener listener;

    public WorkoutAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Workout> workouts, Map<String, Double> progress) {
        items.clear();
        if (workouts != null) {
            items.addAll(workouts);
        }
        progressByWorkout.clear();
        if (progress != null) {
            progressByWorkout.putAll(progress);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutBinding binding = ItemWorkoutBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemWorkoutBinding binding;

        ViewHolder(ItemWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Workout workout) {
            binding.title.setText(workout.title);

            List<String> parts = new ArrayList<>();
            int count = workout.exercisesCount == null ? 0 : workout.exercisesCount;
            parts.add(count + (count == 1 ? " exercício" : " exercícios"));
            if (workout.duration != null && workout.duration > 0) {
                parts.add(workout.duration + " min");
            }
            if (workout.difficulty != null && !workout.difficulty.isEmpty()) {
                parts.add(workout.difficulty);
            }
            binding.subtitle.setText(join(parts));

            double percentage = progressByWorkout.containsKey(workout.id)
                    ? progressByWorkout.get(workout.id) : 0d;
            binding.progressBar.setProgress((int) Math.round(percentage));
            binding.progressLabel.setText(String.format(Locale.getDefault(), "%.0f%%", percentage));

            binding.getRoot().setOnClickListener(v -> listener.onOpen(workout));
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onLongPress(workout);
                return true;
            });
        }

        private String join(List<String> parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(part);
            }
            return sb.toString();
        }
    }
}
