package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class ExerciseCheck {
    public String id;
    @SerializedName("exercise_id") public String exerciseId;
    @SerializedName("workout_id") public String workoutId;
    public Integer year;
    public Integer week;
    public boolean checked;
}
