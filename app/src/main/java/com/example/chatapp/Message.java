package com.example.chatapp;

public class Message {

    private String content;
    private int sender;

    public Message(){

    }
    public Message(String content, int sender){
        this.content = content;
        this.sender = sender;
    }

    public void setContent(String content, String username) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }
}
