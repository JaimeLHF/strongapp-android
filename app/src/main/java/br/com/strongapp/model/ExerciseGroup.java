package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ExerciseGroup {
    public String id;
    public String name;
    @SerializedName("order_index") public Integer orderIndex;
    @SerializedName("workout_exercises") public List<WorkoutExercise> workoutExercises;
}
