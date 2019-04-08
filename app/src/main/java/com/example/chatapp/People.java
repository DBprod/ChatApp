package com.example.chatapp;

public class People {
    private String name;
    private String uid;
    private String mod;
    private String exp;

    public People(){}

    public People (String name, String uid, String mod, String exp){
        this.name = name;
        this.uid = uid;
        this.mod = mod;
        this.exp = exp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getMod(){
        return mod;
    }

    public void setMod(String publicKey) {
        this.mod = publicKey;
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
