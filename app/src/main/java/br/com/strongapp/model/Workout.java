package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Workout {
    public String id;
    public String title;
    public String description;
    public Integer duration;
    public String difficulty;
    @SerializedName("exercises_count") public Integer exercisesCount;
    @SerializedName("created_at") public String createdAt;
    @SerializedName("workout_exercises") public List<WorkoutExercise> workoutExercises;
    @SerializedName("exercise_groups") public List<ExerciseGroup> exerciseGroups;
}
