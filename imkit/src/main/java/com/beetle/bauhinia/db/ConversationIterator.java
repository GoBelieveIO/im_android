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

                RandomAccessFile f = new RandomAccessFile(file, "r");
                MessageIterator iter = new MessageIterator(f);
                IMessage msg = iter.next();
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
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
        return null;
    }
}

