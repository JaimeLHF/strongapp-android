package br.com.strongapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.databinding.ActivityDiaryBinding;
import br.com.strongapp.databinding.DialogDiaryBinding;
import br.com.strongapp.model.ApiMessage;
import br.com.strongapp.model.DiaryEntry;
import br.com.strongapp.model.DiaryRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Diário do treino: anotações com nota de 1 a 5, como na versão web. */
public class DiaryActivity extends AppCompatActivity implements DiaryAdapter.Listener {

    public static final String EXTRA_ID = "workout_id";
    public static final String EXTRA_TITLE = "workout_title";

    private ActivityDiaryBinding binding;
    private DiaryAdapter adapter;
    private String workoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDiaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        workoutId = getIntent().getStringExtra(EXTRA_ID);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null && !title.isEmpty()) {
            binding.toolbar.setSubtitle(title);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new DiaryAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.swipe.setOnRefreshListener(this::load);
        binding.addButton.setOnClickListener(v -> showEditor(null));

        load();
    }

    private void load() {
        binding.swipe.setRefreshing(true);
        ApiClient.api(this).diary(workoutId).enqueue(new Callback<List<DiaryEntry>>() {
            @Override
            public void onResponse(@NonNull Call<List<DiaryEntry>> call, @NonNull Response<List<DiaryEntry>> response) {
                binding.swipe.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submit(response.body());
                    binding.emptyState.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DiaryEntry>> call, @NonNull Throwable t) {
                binding.swipe.setRefreshing(false);
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    /** {@code entry} nulo cria uma anotação; preenchido edita a existente. */
    private void showEditor(@Nullable DiaryEntry entry) {
        DialogDiaryBinding dialog = DialogDiaryBinding.inflate(getLayoutInflater());
        if (entry != null) {
            dialog.notesInput.setText(entry.notes);
            dialog.ratingInput.setRating(entry.rating == null ? 5 : entry.rating);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(entry == null ? R.string.diary_new_entry : R.string.diary_edit_entry)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (d, which) -> {
                    String notes = dialog.notesInput.getText() == null
                            ? "" : dialog.notesInput.getText().toString().trim();
                    if (notes.isEmpty()) {
                        toast(getString(R.string.diary_notes_required));
                        return;
                    }
                    int rating = Math.max(1, Math.round(dialog.ratingInput.getRating()));
                    save(entry, new DiaryRequest(notes, rating));
                })
                .show();
    }

    private void save(@Nullable DiaryEntry entry, DiaryRequest body) {
        Call<DiaryEntry> call = entry == null
                ? ApiClient.api(this).addDiaryEntry(workoutId, body)
                : ApiClient.api(this).updateDiaryEntry(entry.id, body);

        call.enqueue(new Callback<DiaryEntry>() {
            @Override
            public void onResponse(@NonNull Call<DiaryEntry> call, @NonNull Response<DiaryEntry> response) {
                if (response.isSuccessful()) {
                    load();
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<DiaryEntry> call, @NonNull Throwable t) {
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    @Override
    public void onEntryLongPress(DiaryEntry entry) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.workout_diary)
                .setItems(new String[]{getString(R.string.edit), getString(R.string.delete)},
                        (dialog, which) -> {
                            if (which == 0) {
                                showEditor(entry);
                            } else {
                                delete(entry);
                            }
                        })
                .show();
    }

    private void delete(DiaryEntry entry) {
        ApiClient.api(this).deleteDiaryEntry(entry.id).enqueue(new Callback<ApiMessage>() {
            @Override
            public void onResponse(@NonNull Call<ApiMessage> call, @NonNull Response<ApiMessage> response) {
                if (response.isSuccessful()) {
                    load();
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiMessage> call, @NonNull Throwable t) {
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
