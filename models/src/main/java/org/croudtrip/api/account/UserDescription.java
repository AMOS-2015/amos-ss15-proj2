package org.croudtrip.api.account;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.Date;

/**
 * A user which can be (but has not been) registered.
 */
public class UserDescription {

    private final String password, email, firstName, lastName, phoneNumber, address, avatarUrl;
    private final Boolean isMale;
    private final Date birthday;


    public UserDescription(
            String email,
            String firstName,
            String lastName,
            String password) {

        this(email, firstName, lastName, password, null, null, null, null, null);
    }

    @JsonCreator
    public UserDescription(
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("password") String password,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("isMale") Boolean isMale,
            @JsonProperty("birthday") Date birthday,
            @JsonProperty("address") String address,
            @JsonProperty("avatarUrl") String avatarUrl) {

        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.isMale = isMale;
        this.birthday = birthday;
    }

    public String getPassword() {
        return password;
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

    public String getAddress() {
        return address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Boolean getIsMale() {
        return isMale;
    }

    public Date getBirthday() {
        return birthday;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDescription that = (UserDescription) o;
        return Objects.equal(password, that.password) &&
                Objects.equal(email, that.email) &&
                Objects.equal(firstName, that.firstName) &&
                Objects.equal(lastName, that.lastName) &&
                Objects.equal(phoneNumber, that.phoneNumber) &&
                Objects.equal(address, that.address) &&
                Objects.equal(avatarUrl, that.avatarUrl) &&
                Objects.equal(isMale, that.isMale) &&
                Objects.equal(birthday, that.birthday);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(password, email, firstName, lastName, phoneNumber, address, avatarUrl, isMale, birthday);
    }
}
