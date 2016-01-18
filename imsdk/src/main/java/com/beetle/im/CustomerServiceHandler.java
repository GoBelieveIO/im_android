package com.beetle.im;

/**
 * Created by houxh on 16/1/17.
 */
public interface CustomerServiceHandler {
    public boolean handleMessage(IMMessage msg, long uid);
    public boolean handleMessageACK(int msgLocalID, long uid);
    public boolean handleMessageFailure(int msgLocalID, long uid);
}
