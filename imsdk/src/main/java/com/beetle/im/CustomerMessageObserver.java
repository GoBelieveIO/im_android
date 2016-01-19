package com.beetle.im;

/**
 * Created by houxh on 16/1/18.
 */
public interface CustomerMessageObserver {
    public void onCustomerMessage(CustomerMessage msg);
    public void onCustomerMessageACK(int msgLocalID, long uid);
    public void onCustomerMessageFailure(int msgLocalID, long uid);
}
