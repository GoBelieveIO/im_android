package com.beetle.bauhinia.db;

/**
 * Created by houxh on 2017/11/13.
 */

public interface IMessageDB {

    //获取最近的消息
    MessageIterator newMessageIterator(long conversationID);
    //获取之前的消息
    MessageIterator newForwardMessageIterator(long conversationID, long firstMsgID);
    //获取之后的消息
    MessageIterator newBackwardMessageIterator(long conversationID, long msgID);
    //获取前后的消息
    MessageIterator newMiddleMessageIterator(long conversationID, long msgID);

    boolean clearConversation(long conversationID);
    void saveMessageAttachment(IMessage msg, String address);
    void saveMessage(IMessage imsg);
    void removeMessage(IMessage imsg);
    void markMessageListened(IMessage imsg);
    void markMessageFailure(IMessage imsg);
    void eraseMessageFailure(IMessage imsg);
}
