package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */

public class IMMessage {
    public int msgLocalID;
    public boolean secret;//点对点加密消息
    public String plainContent;
    public long sender;
    public long receiver;
    public int timestamp;
    public String content;

    //是否由当前用户在当前设备所发出
    public boolean isSelf;
}

