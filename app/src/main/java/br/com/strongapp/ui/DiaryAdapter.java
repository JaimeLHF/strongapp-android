package br.com.strongapp.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.databinding.ItemDiaryBinding;
import br.com.strongapp.model.DiaryEntry;

import java.util.ArrayList;
import java.util.List;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.ViewHolder> {

    public interface Listener {
        void onEntryLongPress(DiaryEntry entry);
    }

    private final List<DiaryEntry> items = new ArrayList<>();
    private final Listener listener;

    public DiaryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<DiaryEntry> entries) {
        items.clear();
        if (entries != null) {
            items.addAll(entries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemDiaryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** "★★★☆☆" para uma nota de 3. Sem nota, string vazia. */
    static String stars(Integer rating) {
        if (rating == null || rating <= 0) return "";
        int filled = Math.min(rating, 5);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            builder.append(i < filled ? '★' : '☆');
        }
        return builder.toString();
    }

    /** "2026-09-05" vira "05/09/2026"; qualquer outro formato passa direto. */
    static String date(String raw) {
        if (raw == null || raw.length() < 10) return raw == null ? "" : raw;
        String day = raw.substring(8, 10);
        String month = raw.substring(5, 7);
        String year = raw.substring(0, 4);
        return day + "/" + month + "/" + year;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemDiaryBinding binding;

        ViewHolder(ItemDiaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DiaryEntry entry, Listener listener) {
            binding.date.setText(date(entry.entryDate == null ? entry.createdAt : entry.entryDate));
            binding.stars.setText(stars(entry.rating));
            binding.notes.setText(entry.notes == null ? "" : entry.notes);
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onEntryLongPress(entry);
                return true;
            });
        }
    }
}
