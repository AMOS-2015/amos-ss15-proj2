package org.croudtrip.auth;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

/**
 * One registered user
 */
public class User extends AbstractUser {

    @NotNull private final String id;


    @JsonCreator
    public User(
            @JsonProperty("id") String id,
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName) {

        super(email, firstName, lastName);
        this.id = id;
    }


    public String getId() {
        return id;
    }


    @Override
    public boolean equals(Object other) {
        if (!super.equals(other) || !(other instanceof User)) return false;
        User user = (User) other;
        return Objects.equal(id, user.id);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), id);
    }

}
