package br.com.strongapp.ui;

import br.com.strongapp.model.Exercise;

/** Exercício escolhido na montagem do treino, com os números editáveis pelo usuário. */
public class PickedExercise {
    public final Exercise exercise;
    public Integer sets = 3;
    public Integer reps = 12;
    public Double weight = null;
    public Integer restTime = 60;

    public PickedExercise(Exercise exercise) {
        this.exercise = exercise;
    }
}
