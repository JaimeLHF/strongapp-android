package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class CheckRequest {
    @SerializedName("exercise_id") public String exerciseId;
    public int year;
    public int week;
    public boolean checked;

    public CheckRequest(String exerciseId, int year, int week, boolean checked) {
        this.exerciseId = exerciseId;
        this.year = year;
        this.week = week;
        this.checked = checked;
    }
}
