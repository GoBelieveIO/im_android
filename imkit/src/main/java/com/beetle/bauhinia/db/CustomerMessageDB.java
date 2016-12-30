package com.beetle.bauhinia.db;

import android.util.Log;

import com.beetle.im.BytePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 16/1/18.
 * FileCustomerMessageDB vs SQLCustomerMessageDB
 */
public class CustomerMessageDB extends FileCustomerMessageDB {

    private static CustomerMessageDB instance = new CustomerMessageDB();

    public static CustomerMessageDB getInstance() {
        return instance;
    }

}
