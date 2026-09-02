package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class ProfileUpdateRequest {
    @SerializedName("first_name") public String firstName;
    @SerializedName("last_name") public String lastName;

    public ProfileUpdateRequest(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
