package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class DiaryEntry {
    public String id;
    @SerializedName("workout_id") public String workoutId;
    public String notes;
    public Integer rating;
    @SerializedName("entry_date") public String entryDate;
    @SerializedName("created_at") public String createdAt;
}
