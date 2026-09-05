package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class AchievementStats {
    @SerializedName("total_workouts") public int totalWorkouts;
    @SerializedName("total_exercise_checks") public int totalExerciseChecks;
    @SerializedName("total_diary_entries") public int totalDiaryEntries;
    @SerializedName("current_streak") public int currentStreak;
}
