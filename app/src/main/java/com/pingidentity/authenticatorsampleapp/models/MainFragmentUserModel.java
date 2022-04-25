package com.pingidentity.authenticatorsampleapp.models;

import com.google.gson.annotations.SerializedName;

public class MainFragmentUserModel {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private Username username;

    private String nickname;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Username getUsername() {
        return username;
    }

    public void setUsername(Username username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public class Username {
        @SerializedName("given")
        private String given;
        @SerializedName("family")
        private String family;

        public Username(String given, String family){
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
