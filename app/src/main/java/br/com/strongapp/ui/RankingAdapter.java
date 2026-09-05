package br.com.strongapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.com.strongapp.databinding.ItemRankingBinding;

import java.util.ArrayList;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<RankingUser> items = new ArrayList<>();
    private String currentUserName = "";

    public void submit(List<RankingUser> users, String currentUserName) {
        this.currentUserName = currentUserName == null ? "" : currentUserName;
        items.clear();
        items.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemRankingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), currentUserName);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemRankingBinding binding;

        ViewHolder(ItemRankingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RankingUser user, String currentUserName) {
            binding.position.setText(String.valueOf(user.position));
            binding.initials.setText(user.initials());
            binding.name.setText(user.name);
            binding.subtitle.setText(user.subtitle);
            binding.value.setText(String.valueOf(user.value));
            binding.youBadge.setVisibility(
                    user.name.equalsIgnoreCase(currentUserName) ? View.VISIBLE : View.GONE);
        }
    }
}
