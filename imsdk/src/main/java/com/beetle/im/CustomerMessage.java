package com.beetle.im;

/**
 * Created by houxh on 16/1/19.
 */
public class CustomerMessage {
    //未被序列化
    public int msgLocalID;

    public long customerAppID;
    public long customerID;
    public long storeID;
    public long sellerID;
    public int timestamp;
    public String content;

    //消息由本设备发出，则不需要重新入库，用于纠正消息标志位
    public boolean isSelf;
}
