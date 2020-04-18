package com.beetle.bauhinia.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 15/3/9.
 */
public interface ConversationIterator {
    public IMessage next();
}

