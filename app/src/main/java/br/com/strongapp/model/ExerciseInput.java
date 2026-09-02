package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class ExerciseInput {
    @SerializedName("exercise_id") public String exerciseId;
    public Integer sets;
    public Integer reps;
    public Double weight;
    @SerializedName("rest_time") public Integer restTime;
    @SerializedName("order_index") public Integer orderIndex;
}
