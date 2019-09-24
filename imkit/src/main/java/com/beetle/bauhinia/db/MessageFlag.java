package com.beetle.bauhinia.db;



public class MessageFlag {
    public static final int MESSAGE_FLAG_DELETE = 1;
    public static final int MESSAGE_FLAG_ACK = 2;
    //public static final int MESSAGE_FLAG_PEER_ACK = 4;
    public static final int MESSAGE_FLAG_FAILURE = 8;
    public static final int MESSAGE_FLAG_LISTENED = 16;
}