package com.beetle.bauhinia.db.message;



public  abstract class Notification extends MessageContent {
    public String description;

    public String getDescription() {
        return this.description;
    }
}
