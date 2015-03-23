package com.beetle.bauhinia.db;

import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 15/3/21.
 */
public class MessageIterator {
    private ReverseFile revFile;

    public MessageIterator(RandomAccessFile f) throws IOException {
        if (!MessageDB.checkHeader(f)) {
            Log.i("imservice", "check header fail");
            return;
        }
        this.revFile = new ReverseFile(f);
    }

    public MessageIterator(RandomAccessFile f, int lastMsgID) throws IOException {
        if (!MessageDB.checkHeader(f)) {
            Log.i("imservice", "check header fail");
            return;
        }
        this.revFile = new ReverseFile(f, lastMsgID);
    }

    public IMessage next() {
        if (this.revFile == null) return null;
        return MessageDB.readMessage(this.revFile);
    }
}

