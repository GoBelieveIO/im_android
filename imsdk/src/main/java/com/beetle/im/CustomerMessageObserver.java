package com.beetle.im;

/**
 * Created by houxh on 16/1/18.
 */
public interface CustomerMessageObserver {
    public void onCustomerSupportMessage(CustomerMessage msg);
    public void onCustomerMessage(CustomerMessage msg);
    public void onCustomerMessageACK(CustomerMessage msg);
    public void onCustomerMessageFailure(CustomerMessage msg);
}
