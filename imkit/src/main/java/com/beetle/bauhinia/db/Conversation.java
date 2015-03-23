package com.beetle.bauhinia.db;

/**
 * Created by houxh on 15/3/9.
 */
public class Conversation {
    public static final int CONVERSATION_PEER = 1;
    public static final int CONVERSATION_GROUP = 2;

    public int type;
    public long cid;
    public IMessage message;
    public String name;
    public String avatar;
}
