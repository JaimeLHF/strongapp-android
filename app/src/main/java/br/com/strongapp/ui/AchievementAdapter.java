package br.com.strongapp.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.R;
import br.com.strongapp.databinding.ItemAchievementBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista de conquistas: as desbloqueadas ficam em destaque, as demais mostram o progresso. */
public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<Achievement> items = new ArrayList<>();

    public void submit(List<Achievement> achievements) {
        items.clear();
        items.addAll(achievements);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAchievementBinding binding = ItemAchievementBinding.inflate(
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

    private static int themeColor(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant, value, true);
        return value.data;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAchievementBinding binding;

        ViewHolder(ItemAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Achievement achievement) {
            binding.name.setText(achievement.name);
            binding.description.setText(achievement.description);
            binding.icon.setImageResource(achievement.icon);
            binding.badge.setVisibility(achievement.unlocked ? View.VISIBLE : View.GONE);

            boolean showProgress = !achievement.unlocked && achievement.total > 0;
            binding.progressBox.setVisibility(showProgress ? View.VISIBLE : View.GONE);
            if (showProgress) {
                binding.progressLabel.setText(String.format(Locale.getDefault(), "%d/%d",
                        achievement.progress, achievement.total));
                binding.progressBar.setProgress(achievement.progress * 100 / achievement.total);
            }

            // Desbloqueada em laranja; bloqueada no cinza do tema, como na versão web.
            Context context = binding.getRoot().getContext();
            binding.icon.setColorFilter(achievement.unlocked
                    ? ContextCompat.getColor(context, R.color.brand_primary)
                    : themeColor(context));
            binding.getRoot().setAlpha(achievement.unlocked ? 1f : 0.75f);
        }
    }
}
