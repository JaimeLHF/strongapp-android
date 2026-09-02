package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class WorkoutExercise {
    public String id;
    @SerializedName("exercise_id") public String exerciseId;
    @SerializedName("group_id") public String groupId;
    public Integer sets;
    public Integer reps;
    public Double weight;
    @SerializedName("rest_time") public Integer restTime;
    @SerializedName("order_index") public Integer orderIndex;
    public Exercise exercise;
}
