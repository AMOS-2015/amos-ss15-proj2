package org.croudtrip.auth;


import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

/**
 * Collection of properties common to users.
 */
abstract class AbstractUser {

    @NotNull private final String email;
    @NotNull private final String firstName;
    @NotNull private final String lastName;

    public AbstractUser(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
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


    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof AbstractUser)) return false;
        AbstractUser user = (AbstractUser) other;
        return Objects.equal(email, user.email)
                && Objects.equal(firstName, user.firstName)
                && Objects.equal(lastName, user.lastName);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(email, firstName, lastName);
    }

}
