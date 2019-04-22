/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo.model;

import com.beetle.bauhinia.db.IMessage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

/**
 * Created by houxh on 15/3/9.
 */
public class Conversation {
    public static final int CONVERSATION_PEER = 1;
    public static final int CONVERSATION_GROUP = 2;
    public static final int CONVERSATION_CUSTOMER_SERVICE = 3;
    public static final int CONVERSATION_PEER_SECRET = 4;//点对点加密会话

    public static final int STATE_UNINITIALIZE = 0;//未初始化状态
    public static final int STATE_WAIT = 1;//等待对方上线
    public static final int STATE_EXCHANGE = 2;//交换密钥中,暂时未被使用
    public static final int STATE_CONNECTED = 3;//连接成功，可以发送加密消息


    public long rowid;
    public int type;
    public long cid;
    public int state;//p2p加密会话的状态,普通会话此字段无意义
    public IMessage message;

    //search
    public ArrayList<IMessage> messages = new ArrayList<>();

    private String name;
    private String avatar;
    private String detail;
    private int unreadCount;

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(
            this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }


    public void setName(String name) {
        String old = this.name;
        this.name = name;
        changeSupport.firePropertyChange("name", old, this.name);
    }

    public String getName() {
        return this.name;
    }

    public void setAvatar(String avatar) {
        String old = this.avatar;
        this.avatar = avatar;
        changeSupport.firePropertyChange("avatar", old, this.avatar);
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setDetail(String detail) {
        String old = this.detail;
        this.detail = detail;
        changeSupport.firePropertyChange("detail", old, this.detail);
    }

    public String getDetail() {
        return this.detail;
    }

    public void setUnreadCount(int unreadCount) {
        int old = this.unreadCount;
        this.unreadCount = unreadCount;
        changeSupport.firePropertyChange("unreadCount", old, this.unreadCount);
    }

    public int getUnreadCount() {
        return this.unreadCount;
    }
}
