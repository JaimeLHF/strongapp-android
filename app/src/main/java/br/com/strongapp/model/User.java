package br.com.strongapp.model;

import com.google.gson.annotations.SerializedName;

public class User {
    public long id;
    public String name;
    public String email;
    @SerializedName("first_name") public String firstName;
    @SerializedName("last_name") public String lastName;
    @SerializedName("avatar_url") public String avatarUrl;
    @SerializedName("created_at") public String createdAt;

    public String displayName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null && !lastName.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(lastName);
        }
        if (sb.length() == 0 && name != null) sb.append(name);
        if (sb.length() == 0 && email != null) sb.append(email);
        return sb.toString();
    }
}
