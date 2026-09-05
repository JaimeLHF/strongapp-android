package br.com.strongapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import br.com.strongapp.R;
import br.com.strongapp.data.ApiClient;
import br.com.strongapp.databinding.ActivityExportBinding;
import br.com.strongapp.model.DiaryEntry;
import br.com.strongapp.model.ExportBundle;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutExercise;
import br.com.strongapp.model.WorkoutProgress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Exportar Dados: baixa tudo de GET /export e gera o relatório em texto ou a planilha
 * CSV, entregando o arquivo pelo compartilhamento do Android — o equivalente nativo
 * ao download do navegador na versão web.
 */
public class ExportActivity extends AppCompatActivity {

    private ActivityExportBinding binding;
    private ExportBundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.reportButton.setOnClickListener(v -> export(false));
        binding.csvButton.setOnClickListener(v -> export(true));

        load();
    }

    private void load() {
        ApiClient.api(this).export().enqueue(new Callback<ExportBundle>() {
            @Override
            public void onResponse(@NonNull Call<ExportBundle> call, @NonNull Response<ExportBundle> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bundle = response.body();
                    binding.summaryLabel.setText(getString(R.string.export_summary,
                            size(bundle.workouts), size(bundle.diaryEntries), size(bundle.progressRecords)));
                } else {
                    toast(ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ExportBundle> call, @NonNull Throwable t) {
                toast(ApiClient.failureMessage(t));
            }
        });
    }

    private void export(boolean csv) {
        if (bundle == null) {
            toast(getString(R.string.export_not_ready));
            load();
            return;
        }
        try {
            File file = write(csv);
            share(file, csv ? "text/csv" : "text/plain");
        } catch (IOException e) {
            toast(getString(R.string.export_failed));
        }
    }

    private File write(boolean csv) throws IOException {
        File dir = new File(getCacheDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta de exportação.");
        }

        String stamp = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        File file = new File(dir, "strongapp-" + stamp + (csv ? ".csv" : ".txt"));

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            if (csv) {
                writeCsv(writer);
            } else {
                writeReport(writer);
            }
        }
        return file;
    }

    /** Mesmas colunas do CSV da versão web. */
    private void writeCsv(Writer writer) throws IOException {
        writer.write("Treino,Dificuldade,Duracao,Exercicio,Series,Reps,Carga,Descanso\n");
        if (bundle.workouts != null) {
            for (Workout workout : bundle.workouts) {
                if (workout.workoutExercises == null || workout.workoutExercises.isEmpty()) {
                    writer.write(csv(workout.title) + "," + csv(workout.difficulty) + ","
                            + value(workout.duration) + ",,,,,\n");
                    continue;
                }
                for (WorkoutExercise item : workout.workoutExercises) {
                    writer.write(csv(workout.title) + ","
                            + csv(workout.difficulty) + ","
                            + value(workout.duration) + ","
                            + csv(item.exercise == null ? "" : item.exercise.name) + ","
                            + value(item.sets) + ","
                            + value(item.reps) + ","
                            + value(item.weight) + ","
                            + value(item.restTime) + "\n");
                }
            }
        }
    }

    private void writeReport(Writer writer) throws IOException {
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        writer.write("RELATÓRIO DE TREINOS — RITMO FORTE GYM\n");
        writer.write("Exportado em: " + today + "\n\n");
        writer.write("RESUMO\n");
        writer.write("Total de treinos: " + size(bundle.workouts) + "\n");
        writer.write("Entradas no diário: " + size(bundle.diaryEntries) + "\n");
        writer.write("Registros de progresso: " + size(bundle.progressRecords) + "\n\n");

        writer.write("SEUS TREINOS\n");
        if (bundle.workouts == null || bundle.workouts.isEmpty()) {
            writer.write("Nenhum treino encontrado.\n");
        } else {
            for (Workout workout : bundle.workouts) {
                writer.write("\n- " + workout.title + "\n");
                writer.write("  Dificuldade: " + text(workout.difficulty) + "\n");
                writer.write("  Duração: " + value(workout.duration) + " min\n");
                if (workout.description != null && !workout.description.isEmpty()) {
                    writer.write("  Descrição: " + workout.description + "\n");
                }
                if (workout.workoutExercises != null) {
                    for (WorkoutExercise item : workout.workoutExercises) {
                        writer.write("    * " + (item.exercise == null ? "?" : item.exercise.name)
                                + " — " + value(item.sets) + "x" + value(item.reps)
                                + (item.weight == null ? "" : " · " + item.weight + "kg")
                                + (item.restTime == null ? "" : " · " + item.restTime + "s descanso") + "\n");
                    }
                }
            }
        }

        writer.write("\nDIÁRIO\n");
        if (bundle.diaryEntries == null || bundle.diaryEntries.isEmpty()) {
            writer.write("Nenhuma anotação.\n");
        } else {
            for (DiaryEntry entry : bundle.diaryEntries) {
                writer.write("- " + DiaryAdapter.date(entry.entryDate) + " "
                        + DiaryAdapter.stars(entry.rating) + "\n  " + text(entry.notes) + "\n");
            }
        }

        writer.write("\nPROGRESSO\n");
        if (bundle.progressRecords == null || bundle.progressRecords.isEmpty()) {
            writer.write("Nenhum registro.\n");
        } else {
            for (WorkoutProgress progress : bundle.progressRecords) {
                writer.write("- Semana " + value(progress.week) + "/" + value(progress.year)
                        + ": " + value(progress.completionPercentage) + "%\n");
            }
        }
    }

    private void share(File file, String mime) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".exports", file);
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.export_data)));
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String text(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Escapa aspas e vírgulas para o campo caber numa célula do CSV. */
    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
