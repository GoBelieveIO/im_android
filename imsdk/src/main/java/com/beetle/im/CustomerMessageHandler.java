package com.beetle.im;

/**
 * Created by houxh on 16/1/17.
 */
public interface CustomerMessageHandler {
    public boolean handleMessage(CustomerMessage msg);
    public boolean handleMessageACK(int msgLocalID, long uid);
    public boolean handleMessageFailure(int msgLocalID, long uid);
}
