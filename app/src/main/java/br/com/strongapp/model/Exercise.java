package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class Exercise {
    public String id;
    public String name;
    @SerializedName("muscle_group") public String muscleGroup;
    public String equipment;
    public String instructions;
    @SerializedName("video_url") public String videoUrl;
}
