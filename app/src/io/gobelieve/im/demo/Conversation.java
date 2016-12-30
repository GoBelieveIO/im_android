package io.gobelieve.im.demo;

import com.beetle.bauhinia.db.IMessage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Created by houxh on 15/3/9.
 */
public class Conversation {
    public static final int CONVERSATION_PEER = 1;
    public static final int CONVERSATION_GROUP = 2;
    public static final int CONVERSATION_CUSTOMER_SERVICE = 3;

    public int type;
    public long cid;
    public IMessage message;
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
