package com.beetle.im;

/**
 * Created by houxh on 16/1/17.
 */
public interface CustomerMessageHandler {
    public boolean handleCustomerSupportMessage(CustomerMessage msg);
    public boolean handleMessage(CustomerMessage msg);
    public boolean handleMessageACK(CustomerMessage msg);
    public boolean handleMessageFailure(CustomerMessage msg);
}
