package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    public String email;
    public String password;
    @SerializedName("first_name") public String firstName;
    @SerializedName("last_name") public String lastName;

    public RegisterRequest(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }
}
