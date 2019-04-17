package com.example.chatapp;

public class People {
    private String contactId;
    private long timestamp;
    private String content;
    private int sender;

    public People(){}

    public People(String contactId, long timestamp, String content, int sender) {
        this.contactId = contactId;
        this.timestamp = timestamp;
        this.content = content;
        this.sender = sender;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }
}
