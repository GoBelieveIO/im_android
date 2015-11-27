package com.beetle.bauhinia.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 15/3/9.
 */
public class ConversationIterator {

    private File[] files;
    private int type;
    private int index;
    public ConversationIterator(File[] files, int type) {
        this.files = files;
        this.type = type;
        index = -1;
    }

    private IMessage getLastMessage(File file) {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            MessageIterator iter = new MessageIterator(f);

            IMessage msg = null;
            while (true) {
                msg = iter.next();
                if (msg == null) {
                    break;
                }

                if (msg.content.getType() != IMessage.MessageType.MESSAGE_ATTACHMENT) {
                    break;
                }
            }
            return msg;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

                IMessage msg = getLastMessage(file);
                if (msg == null) {
                    continue;
                }
                Conversation conv = new Conversation();
                conv.type = this.type;
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

