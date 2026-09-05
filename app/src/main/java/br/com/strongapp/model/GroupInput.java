package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Um superset: nome do bloco e os exercícios executados em sequência (RF07). */
public class GroupInput {
    public String name;
    @SerializedName("order_index") public Integer orderIndex;
    public List<ExerciseInput> exercises;
}
