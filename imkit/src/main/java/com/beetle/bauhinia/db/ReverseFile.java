package com.beetle.bauhinia.db;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by houxh on 14-7-22.
 */
public class ReverseFile {
    private RandomAccessFile file;
    private FileChannel fileChan;
    private long pos;
    public ReverseFile(RandomAccessFile file) throws IOException {
        this.file = file;
        this.fileChan = file.getChannel();
        this.pos = this.file.length();
    }

    public ReverseFile(RandomAccessFile file, long position) throws IOException {
        this.file = file;
        this.fileChan = file.getChannel();
        this.pos = position;
    }

    public long getFilePointer() {
        return pos;
    }

    public int read(byte[] buf) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(buf.length);
        int n = this.fileChan.read(b, pos - buf.length);
        this.pos -= n;
        byte[] t = b.array();
        System.arraycopy(t, 0, buf, 0, t.length);
        return n;
    }
}

