package br.com.strongapp.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.databinding.ItemPickedExerciseBinding;

import java.util.List;
import java.util.Locale;

public class PickedExerciseAdapter extends RecyclerView.Adapter<PickedExerciseAdapter.ViewHolder> {

    public interface Listener {
        void onRemove(int position);
    }

    private final List<PickedExercise> items;
    private final Listener listener;

    public PickedExerciseAdapter(List<PickedExercise> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemPickedExerciseBinding.inflate(
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

        private final ItemPickedExerciseBinding binding;

        ViewHolder(ItemPickedExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PickedExercise picked) {
            binding.name.setText(picked.exercise.name);

            // Solta os observadores da linha anterior antes de reescrever os campos,
            // senão o RecyclerView grava os valores reciclados no exercício errado.
            detach(binding.setsInput);
            detach(binding.repsInput);
            detach(binding.weightInput);
            detach(binding.restInput);

            setText(binding.setsInput, picked.sets == null ? "" : String.valueOf(picked.sets));
            setText(binding.repsInput, picked.reps == null ? "" : String.valueOf(picked.reps));
            setText(binding.weightInput, picked.weight == null
                    ? "" : String.format(Locale.US, "%.0f", picked.weight));
            setText(binding.restInput, picked.restTime == null ? "" : String.valueOf(picked.restTime));

            watch(binding.setsInput, value -> picked.sets = toInt(value));
            watch(binding.repsInput, value -> picked.reps = toInt(value));
            watch(binding.weightInput, value -> picked.weight = toDouble(value));
            watch(binding.restInput, value -> picked.restTime = toInt(value));

            binding.removeButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRemove(position);
                }
            });
        }

        private void detach(EditText field) {
            Object previous = field.getTag();
            if (previous instanceof TextWatcher) {
                field.removeTextChangedListener((TextWatcher) previous);
                field.setTag(null);
            }
        }

        private void setText(EditText field, String value) {
            if (!value.equals(field.getText().toString())) {
                field.setText(value);
            }
        }

        private void watch(EditText field, Consumer consumer) {
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    consumer.accept(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            };
            field.addTextChangedListener(watcher);
            field.setTag(watcher);
        }

        private Integer toInt(String value) {
            try {
                return value.trim().isEmpty() ? null : Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Double toDouble(String value) {
            try {
                return value.trim().isEmpty() ? null : Double.parseDouble(value.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    interface Consumer {
        void accept(String value);
    }
}
