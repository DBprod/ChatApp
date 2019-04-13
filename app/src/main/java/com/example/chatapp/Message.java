package com.example.chatapp;

public class Message {

    private String content;
    private int sender;
    private boolean emoji;

    public Message(){

    }
    public Message(String content, int sender, boolean isEmoji){
        this.content = content;
        this.sender = sender;
        this.emoji = emoji;
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

    public boolean isEmoji() {
        return emoji;
    }

    public void setEmoji(boolean emoji) {
        this.emoji = emoji;
    }
}
