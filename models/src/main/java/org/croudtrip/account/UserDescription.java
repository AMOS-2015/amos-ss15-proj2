package org.croudtrip.account;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

/**
 * A user which can be (but has not been) registered.
 */
public class UserDescription {

    @NotNull private final String password;
    @NotNull public final String email;
    @NotNull private final  String firstName;
    @NotNull private final String lastName;

    @JsonCreator
    public UserDescription(
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("password") String password) {

        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }


    public String getEmail() {
        return email;
    }


    public String getFirstName() {
        return firstName;
    }


    public String getLastName() {
        return lastName;
    }


    public String getPassword() {
        return password;
    }


    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof UserDescription)) return false;
        UserDescription user = (UserDescription) other;
        return Objects.equal(password, user.password)
                && Objects.equal(email, user.email)
                && Objects.equal(firstName, user.firstName)
                && Objects.equal(lastName, user.lastName);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(email, firstName, lastName, password);
    }

}
