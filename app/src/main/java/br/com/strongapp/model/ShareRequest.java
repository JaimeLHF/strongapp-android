package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class ShareRequest {
    @SerializedName("workout_id") public String workoutId;
    public String title;
    @SerializedName("is_public") public boolean isPublic = true;

    public ShareRequest(String workoutId, String title) {
        this.workoutId = workoutId;
        this.title = title;
    }
}
