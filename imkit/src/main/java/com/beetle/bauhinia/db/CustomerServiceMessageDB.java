package com.beetle.bauhinia.db;

import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerServiceMessageDB extends MessageDB {
    private static CustomerServiceMessageDB instance = new CustomerServiceMessageDB();

    public static CustomerServiceMessageDB getInstance() {
        return instance;
    }

    //普通用户客服消息存储使用聚合模式
    private boolean aggregationMode = true;

    private File dir;

    public void setDir(File dir) {
        this.dir = dir;
    }

    public void setAggregationMode(boolean aggregation) {
        aggregationMode = aggregation;
    }

    private String fileName(long uid) {
        if (aggregationMode) {
            return "0";
        } else {
            return "" + uid;
        }
    }

    public boolean insertMessage(IMessage msg, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            boolean b = insertMessage(f, msg);
            f.close();
            return b;
        } catch (Exception e) {
            Log.i("imservice", "excp:" + e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean acknowledgeMessage(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_ACK);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean markMessageFailure(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_FAILURE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean eraseMessageFailure(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            eraseFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_FAILURE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeMessage(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_DELETE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clearCoversation(long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    public MessageIterator newMessageIterator(long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "r");
            return new MessageIterator(f);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public MessageIterator newMessageIterator(long uid, int firstMsgID) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "r");
            return new MessageIterator(f, firstMsgID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConversationIterator newConversationIterator() {
        return new ConversationIterator(this.dir.listFiles(), Conversation.CONVERSATION_PEER);
    }
}
