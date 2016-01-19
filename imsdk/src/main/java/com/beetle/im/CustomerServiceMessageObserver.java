package com.beetle.im;

/**
 * Created by houxh on 16/1/18.
 */
public interface CustomerServiceMessageObserver {
    public void onCustomerServiceMessage(CustomerMessage msg);
    public void onCustomerServiceMessageACK(int msgLocalID, long uid);
    public void onCustomerServiceMessageFailure(int msgLocalID, long uid);
}
