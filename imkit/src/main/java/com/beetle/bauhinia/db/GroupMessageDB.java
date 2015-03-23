package com.beetle.bauhinia.db;

import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 15/3/21.
 */




public class GroupMessageDB extends MessageDB {
    private static GroupMessageDB instance = new GroupMessageDB();

    public static GroupMessageDB getInstance() {
        return instance;
    }

    private File dir;

    public void setDir(File dir) {
        this.dir = dir;
    }

    private String fileName(long gid) {
        return ""+gid;
    }

    public boolean insertMessage(IMessage msg, long gid) {
        try {
            File file = new File(this.dir, fileName(gid));
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

    public boolean acknowledgeMessage(int msgLocalID, long gid) {
        try {
            File file = new File(this.dir, fileName(gid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_ACK);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean markMessageFailure(int msgLocalID, long gid) {
        try {
            File file = new File(this.dir, fileName(gid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_FAILURE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeMessage(int msgLocalID, long gid) {
        try {
            File file = new File(this.dir, fileName(gid));
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
        return new ConversationIterator(this.dir.listFiles(), Conversation.CONVERSATION_GROUP);
    }

}
