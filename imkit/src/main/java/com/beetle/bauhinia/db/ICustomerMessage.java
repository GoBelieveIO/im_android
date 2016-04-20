package com.beetle.bauhinia.db;


/**
 * Created by houxh on 14-7-22.
 */


public class ICustomerMessage  extends IMessage {
    public long customerAppID;
    public long customerID;
    public long storeID;
    public long sellerID;
    public boolean isSupport;//是否发自客服人员
}
