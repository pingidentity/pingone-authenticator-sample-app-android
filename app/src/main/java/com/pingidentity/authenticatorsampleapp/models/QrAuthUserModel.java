package com.pingidentity.authenticatorsampleapp.models;

import com.google.gson.annotations.SerializedName;

/*
 * UserModel the class that represents a simple user model retrieved from
 * the server during authentication as a JsonObject inside a JsonArray
 */
public class QrAuthUserModel {

    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private FullName fullName;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public FullName getFullName() {
        return fullName;
    }

    public void setFullName(FullName fullName) {
        this.fullName = fullName;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public class FullName {

        @SerializedName("given")
        private String given;

        @SerializedName("family")
        private String family;

        public FullName(String given, String family){
            this.given = given;
            this.family = family;
        }

        public String getGiven() {
            return given;
        }

        public void setGiven(String given) {
            this.given = given;
        }

        public String getFamily() {
            return family;
        }

        public void setFamily(String family) {
            this.family = family;
        }
    }
}
