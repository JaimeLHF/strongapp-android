package br.com.strongapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.databinding.ItemExerciseBinding;
import br.com.strongapp.model.Exercise;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {

    /** Toque longo no exercício, usado pela gestão do catálogo (RF04). */
    public interface Listener {
        void onExerciseLongPress(Exercise exercise);
    }

    private final List<Exercise> items = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Exercise> exercises) {
        items.clear();
        if (exercises != null) {
            items.addAll(exercises);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
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

        private final ItemExerciseBinding binding;

        ViewHolder(ItemExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Exercise exercise) {
            binding.name.setText(exercise.name);
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener == null) return false;
                listener.onExerciseLongPress(exercise);
                return true;
            });

            StringBuilder meta = new StringBuilder();
            if (exercise.muscleGroup != null && !exercise.muscleGroup.isEmpty()) {
                meta.append(exercise.muscleGroup);
            }
            if (exercise.equipment != null && !exercise.equipment.isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(exercise.equipment);
            }
            binding.meta.setText(meta.toString());

            boolean hasInstructions = exercise.instructions != null && !exercise.instructions.isEmpty();
            boolean isOpen = expanded.contains(exercise.id);
            binding.instructions.setText(hasInstructions ? exercise.instructions : "");
            binding.instructions.setVisibility(isOpen && hasInstructions ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                if (!hasInstructions) return;
                if (expanded.contains(exercise.id)) {
                    expanded.remove(exercise.id);
                } else {
                    expanded.add(exercise.id);
                }
                notifyItemChanged(getBindingAdapterPosition());
            });
        }
    }
}
