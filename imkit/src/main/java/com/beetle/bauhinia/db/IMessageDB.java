/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;

/**
 * Created by houxh on 2017/11/13.
 */

public interface IMessageDB {

    //获取最近的消息
    MessageIterator newMessageIterator(long conversationID);
    //获取之前的消息
    MessageIterator newForwardMessageIterator(long conversationID, int firstMsgID);
    //获取之后的消息
    MessageIterator newBackwardMessageIterator(long conversationID, int msgID);
    //获取前后的消息
    MessageIterator newMiddleMessageIterator(long conversationID, int msgID);

    boolean clearConversation(long conversationID);
    void saveMessageAttachment(IMessage msg, String address);
    void saveMessage(IMessage imsg);
    void removeMessage(IMessage imsg);
    void markMessageListened(IMessage imsg);
    void markMessageFailure(IMessage imsg);
    void eraseMessageFailure(IMessage imsg);
}
