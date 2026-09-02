package br.com.strongapp.model;

import java.util.List;

public class CreateWorkoutRequest {
    public String title;
    public String description;
    public Integer duration;
    public String difficulty;
    public List<ExerciseInput> exercises;
}
