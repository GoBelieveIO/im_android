package com.beetle.bauhinia.db;
import java.util.List;

/**
 * Created by houxh on 2017/11/13.
 */

public interface IMessageDB {
    List<IMessage> loadConversationData();
    List<IMessage> loadConversationData(int messageID);
    List<IMessage> loadEarlierData(int messageID);
    List<IMessage> loadLateData(int messageID);
    void saveMessageAttachment(IMessage msg, String address);
    void saveMessage(IMessage imsg);
    void removeMessage(IMessage imsg);
    void markMessageListened(IMessage imsg);
    void markMessageFailure(IMessage imsg);
    void eraseMessageFailure(IMessage imsg);
    void clearConversation();
    IMessage newOutMessage();
}
