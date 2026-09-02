package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class WorkoutProgress {
    @SerializedName("workout_id") public String workoutId;
    public Integer year;
    public Integer week;
    @SerializedName("total_exercises") public Integer totalExercises;
    @SerializedName("completed_exercises") public Integer completedExercises;
    @SerializedName("completion_percentage") public Double completionPercentage;
}
