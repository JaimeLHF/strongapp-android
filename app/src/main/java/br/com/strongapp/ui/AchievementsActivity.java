package br.com.strongapp.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.databinding.ActivityAchievementsBinding;
import br.com.strongapp.model.AchievementStats;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Conquistas do usuário, com as mesmas seis regras da versão web. */
public class AchievementsActivity extends AppCompatActivity {

    private ActivityAchievementsBinding binding;
    private AchievementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAchievementsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new AchievementAdapter();
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.swipe.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        binding.swipe.setRefreshing(true);
        ApiClient.api(this).achievementStats().enqueue(new Callback<AchievementStats>() {
            @Override
            public void onResponse(@NonNull Call<AchievementStats> call, @NonNull Response<AchievementStats> response) {
                binding.swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    toast(ApiClient.errorMessage(response));
                    return;
                }
                List<Achievement> achievements = Achievement.from(response.body());
                int unlocked = 0;
                for (Achievement achievement : achievements) {
                    if (achievement.unlocked) unlocked++;
                }
                binding.summaryLabel.setText(
                        getString(R.string.achievements_summary, unlocked, achievements.size()));
                adapter.submit(achievements);
            }

            @Override
            public void onFailure(@NonNull Call<AchievementStats> call, @NonNull Throwable t) {
                binding.swipe.setRefreshing(false);
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
