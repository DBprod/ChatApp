package com.example.chatapp;

public class People {
    private String name;
    private String uid;

    public People(){}

    public People (String name, String uid){
        this.name = name;
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
