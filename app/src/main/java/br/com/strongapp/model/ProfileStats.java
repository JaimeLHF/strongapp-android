package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class ProfileStats {
    @SerializedName("total_workouts") public int totalWorkouts;
    @SerializedName("unique_exercises") public int uniqueExercises;
    @SerializedName("avg_completion") public double avgCompletion;
    @SerializedName("current_streak") public int currentStreak;
}
