package com.example.chatapp;

public class Message {

    private String content;
    private String username;

    public Message(){

    }
    public Message(String content){
        this.content = content;
    }

    public void setContent(String content, String username) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
