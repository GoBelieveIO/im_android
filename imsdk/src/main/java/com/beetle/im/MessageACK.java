package com.beetle.im;


public class MessageACK {
    public int seq;
    public int status;

    public static final int MESSAGE_ACK_SUCCESS  = 0;
    public static final int MESSAGE_ACK_NOT_MY_FRIEND = 1;
    public static final int MESSAGE_ACK_NOT_YOUR_FRIEND = 2;
    public static final int MESSAGE_ACK_IN_YOUR_BLACKLIST = 3;
    public static final int MESSAGE_ACK_NOT_GROUP_MEMBER = 64;
}
