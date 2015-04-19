package org.croudtrip.auth;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * One registered user
 */
@Entity(name = User.ENTITY_NAME)
@Table(name = "users")
@NamedQueries({
        @NamedQuery(
                name = User.QUERY_NAME_FIND_ALL,
                query = "SELECT u FROM " + User.ENTITY_NAME + " u"
        ),
        @NamedQuery(
                name = User.QUERY_NAME_FIND_BY_EMAIL,
                query = "SELECT u FROM " + User.ENTITY_NAME + " u WHERE u.email = :" + User.QUERY_PARAM_MAIL
)
})
public class User {

    public static final String
            ENTITY_NAME = "User",
            COLUMN_ID = "user_id",
            QUERY_NAME_FIND_ALL = "org.croudtrip.auth.User.findAll",
            QUERY_NAME_FIND_BY_EMAIL = "org.croudtrip.auth.User.findMail",
            QUERY_PARAM_MAIL = "email";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COLUMN_ID)
    @NotNull
    private long id;

    @NotNull
    @Column(name = "email", nullable = false)
    public String email;

    @NotNull
    @Column(name = "firstName", nullable = false)
    private String firstName;

    @NotNull
    @Column(name = "lastName", nullable = false)
    private String lastName;

    User() { }

    @JsonCreator
    public User(
            @JsonProperty("id") long id,
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName) {

        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }


    public long getId() {
        return id;
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
        if (other == null || !(other instanceof User)) return false;
        User user = (User) other;
        return Objects.equal(id, user.id)
                && Objects.equal(email, user.email)
                && Objects.equal(firstName, user.firstName)
                && Objects.equal(lastName, user.lastName);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(id, email, firstName, lastName);
    }

}
