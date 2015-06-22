/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.api.account;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
    @Temporal(TemporalType.DATE)
    @JsonSerialize(using = DateSerializer.class)
    @JsonDeserialize(using = DateDeserializer.class)
    private Date birthday;

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
            @JsonProperty("birthday") Date birthday,
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


    public Date getBirthday() {
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

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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


    public static class Builder {

        private long id;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private Boolean isMale;
        private Date birthday;
        private String address;
        private String avatarUrl;
        private long lastModified; // unix timestamp in seconds

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder setIsMale(Boolean isMale) {
            this.isMale = isMale;
            return this;
        }

        public Builder setBirthday(Date birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public Builder setLastModified(long lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public User build() {
            return new User(id, email, firstName,lastName, phoneNumber, isMale, birthday, address, avatarUrl, lastModified);
        }
    }

}
