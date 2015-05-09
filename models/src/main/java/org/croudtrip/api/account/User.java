package org.croudtrip.api.account;


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
            QUERY_NAME_FIND_ALL = "org.croudtrip.api.account.User.findAll",
            QUERY_NAME_FIND_BY_EMAIL = "org.croudtrip.api.account.User.findMail",
            QUERY_PARAM_MAIL = "email";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COLUMN_ID)
    @NotNull
    private long id;

    @Column(name = "email", nullable = false)
    @NotNull
    public String email;

    @Column(name = "first_name", nullable = false)
    @NotNull
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotNull
    private String lastName;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Column(name = "isMale", nullable = true)
    private Boolean isMale;

    @Column(name = "birthday", nullable = true)
    private Long birthday; // unix timestamp in seconds

    @Column(name = "address", nullable = true)
    private String address;

    @Column(name = "avatar_url", nullable = true)
    private String avatarUrl;

    @Column(name = "last_modified", nullable = false)
    private long lastModified; // unix timestamp in seconds

    User() { }

    @JsonCreator
    public User(
            @JsonProperty("id") long id,
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("isMale") Boolean isMale,
            @JsonProperty("birthday") Long birthday,
            @JsonProperty("address") String address,
            @JsonProperty("avatarUrl") String avatarUrl,
            @JsonProperty("lastModified") long lastModified) {

        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.isMale = isMale;
        this.birthday = birthday;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.lastModified = lastModified;
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


    public String getPhoneNumber() {
        return phoneNumber;
    }


    public Boolean getIsMale() {
        return isMale;
    }


    public Long getBirthday() {
        return birthday;
    }


    public String getAddress() {
        return address;
    }


    public String getAvatarUrl() {
        return avatarUrl;
    }


    public long getLastModified() {
        return lastModified;
    }


    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }


    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof User)) return false;
        User user = (User) other;
        return Objects.equal(id, user.id)
                && Objects.equal(email, user.email)
                && Objects.equal(firstName, user.firstName)
                && Objects.equal(lastName, user.lastName)
                && Objects.equal(phoneNumber, user.phoneNumber)
                && Objects.equal(isMale, user.isMale)
                && Objects.equal(birthday, user.birthday)
                && Objects.equal(address, user.address)
                && Objects.equal(avatarUrl, user.avatarUrl)
                && Objects.equal(lastModified, user.lastModified);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(id, email, firstName, lastName, phoneNumber, isMale, birthday, address, avatarUrl, lastModified);
    }

    public String getFullName(){
        String name = "";

        boolean firstNameEmpty = firstName == null || firstName.equals("");
        boolean lastNameEmpty = lastName == null || lastName.equals("");

        if(!firstNameEmpty && !lastNameEmpty){
            name = firstName + " " + lastName;
        }else if(lastNameEmpty){
            name = lastName;
        }else{
            name = firstName;
        }

        return name;
    }

}
