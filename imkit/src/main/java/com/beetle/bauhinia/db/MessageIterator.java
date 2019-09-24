package com.beetle.bauhinia.db;

import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 15/3/21.
 */
public interface MessageIterator {
    public IMessage next();
}

