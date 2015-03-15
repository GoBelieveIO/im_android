package com.beetle.bauhinia.db;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 14-7-22.
 */



class PeerConversationIterator implements ConversationIterator{
    private File[] files;
    private int index;
    public PeerConversationIterator(File[] files) {
        this.files = files;
        index = -1;
    }

    public Conversation next() {
        index++;
        if (files == null || files.length <= index) {
            return null;
        }


        for (; index < files.length; index++) {
            File file = files[index];
            if (!file.isFile()) {
                continue;
            }
            try {
                String name = file.getName();
                long uid = Long.parseLong(name);

                PeerMessageIterator iter = PeerMessageDB.getInstance().newMessageIterator(uid);
                IMessage msg = iter.next();
                Conversation conv = new Conversation();
                conv.cid = uid;
                conv.message = msg;
                return conv;
            }  catch (NumberFormatException e) {
                e.printStackTrace();
                continue;
            }
        }
        return null;
    }
}


public class PeerMessageDB extends MessageDB {

    private static PeerMessageDB instance = new PeerMessageDB();

    public static PeerMessageDB getInstance() {
        return instance;
    }

    private File dir;

    public void setDir(File dir) {
        this.dir = dir;
    }

    private String fileName(long uid) {
        return ""+uid;
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

    public boolean acknowledgeMessageFromRemote(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_PEER_ACK);
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

    public PeerMessageIterator newMessageIterator(long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "r");
            return new PeerMessageIterator(f);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public PeerMessageIterator newMessageIterator(long uid, int firstMsgID) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "r");
            return new PeerMessageIterator(f, firstMsgID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConversationIterator newConversationIterator() {
        return new PeerConversationIterator(this.dir.listFiles());
    }
}
