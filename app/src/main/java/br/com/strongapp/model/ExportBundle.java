package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Retorno de GET /export: tudo que o usuário produziu no app. */
public class ExportBundle {
    public List<Workout> workouts;
    @SerializedName("diary_entries") public List<DiaryEntry> diaryEntries;
    @SerializedName("progress_records") public List<WorkoutProgress> progressRecords;
}
