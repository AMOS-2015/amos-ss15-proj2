package org.croudtrip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A model class to have a unified user that is shared between server and client.
 * Created by Frederik Simon on 17.04.2015.
 */
public class User {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;

    @JsonCreator
    public User(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("email") String email,
            @JsonProperty("password") String password) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;

        /* TODO: Don't send password as clear text to server, but only use hash values. But sufficient for first release.*/
        this.password = password;
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

}
