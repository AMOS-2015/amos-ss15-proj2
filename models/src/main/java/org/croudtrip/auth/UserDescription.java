package org.croudtrip.auth;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

/**
 * A user which can be (but has not been) registered.
 */
public class UserDescription extends AbstractUser {

    @NotNull private final String password;

    @JsonCreator
    public UserDescription(
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("password") String password) {

        super(email, firstName, lastName);
        this.password = password;
    }


    public String getPassword() {
        return password;
    }


    @Override
    public boolean equals(Object other) {
        if (!super.equals(other) || !(other instanceof UserDescription)) return false;
        UserDescription user = (UserDescription) other;
        return Objects.equal(password, user.password);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), password);
    }

}
