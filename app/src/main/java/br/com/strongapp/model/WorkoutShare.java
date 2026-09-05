package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class WorkoutShare {
    public String id;
    @SerializedName("share_token") public String shareToken;
    public String title;
    @SerializedName("view_count") public Integer viewCount;
}
