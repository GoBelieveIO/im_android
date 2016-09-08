package com.beetle.bauhinia.db;

import android.util.Log;

import com.beetle.im.BytePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageDB extends MessageDB {

    private class CustomerMessageIterator implements MessageIterator{
        private ReverseFile revFile;

        public CustomerMessageIterator(RandomAccessFile f) throws IOException {
            if (!MessageDB.checkHeader(f)) {
                Log.i("imservice", "check header fail");
                return;
            }
            this.revFile = new ReverseFile(f);
        }

        public CustomerMessageIterator(RandomAccessFile f, int lastMsgID) throws IOException {
            if (!MessageDB.checkHeader(f)) {
                Log.i("imservice", "check header fail");
                return;
            }
            this.revFile = new ReverseFile(f, lastMsgID);
        }

        public IMessage next() {
            if (this.revFile == null) return null;
            return CustomerMessageDB.readMessage(this.revFile);
        }
    }

    public class CustomerConversationIterator implements ConversationIterator {

        private File[] files;
        private int type;
        private int index;
        public CustomerConversationIterator(File[] files, int type) {
            this.files = files;
            this.type = type;
            index = -1;
        }

        private IMessage getLastMessage(File file) {
            try {
                RandomAccessFile f = new RandomAccessFile(file, "r");
                MessageIterator iter = new CustomerMessageIterator(f);

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


    private static CustomerMessageDB instance = new CustomerMessageDB();

    public static CustomerMessageDB getInstance() {
        return instance;
    }

    private File dir;

    public void setDir(File dir) {
        this.dir = dir;
    }

    private String fileName(long uid) {
        return "" + uid;
    }

    public static boolean writeMessage(RandomAccessFile f, IMessage m) {
        try {

            ICustomerMessage msg = (ICustomerMessage)m;

            byte[] buf = new byte[64 * 1024];
            int pos = 0;

            byte[] content = msg.content.raw.getBytes("UTF-8");
            int len = content.length + 4 + 4 + 8 + 8 + 8 + 8 + 1;
            if (4 + 4 + len + 4 + 4 > 64 * 1024) return false;

            BytePacket.writeInt32(IMMAGIC, buf, pos);
            pos += 4;
            BytePacket.writeInt32(len, buf, pos);
            pos += 4;
            BytePacket.writeInt32(msg.flags, buf, pos);
            pos += 4;
            BytePacket.writeInt32(msg.timestamp, buf, pos);
            pos += 4;


            BytePacket.writeInt64(msg.customerAppID, buf, pos);
            pos += 8;
            BytePacket.writeInt64(msg.customerID, buf, pos);
            pos += 8;
            BytePacket.writeInt64(msg.storeID, buf, pos);
            pos += 8;
            BytePacket.writeInt64(msg.sellerID, buf, pos);
            pos += 8;
            buf[pos] = (byte)(msg.isSupport ? 1 : 0);
            pos += 1;

            System.arraycopy(content, 0, buf, pos, content.length);
            pos += content.length;
            BytePacket.writeInt32(len, buf, pos);
            pos += 4;
            BytePacket.writeInt32(IMMAGIC, buf, pos);

            f.write(buf, 0, 4 + 4 + len + 4 + 4);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static IMessage readMessage(ReverseFile file) {
        try {
            byte[] buf = new byte[8];
            int n = file.read(buf);
            if (n != 8) {
                return null;
            }
            int len = BytePacket.readInt32(buf, 0);
            int magic = BytePacket.readInt32(buf, 4);
            if (magic != MessageDB.IMMAGIC) {
                return null;
            }

            buf = new byte[len + 8];
            n = file.read(buf);
            if (n != buf.length) {
                return null;
            }

            ICustomerMessage msg = new ICustomerMessage();
            msg.msgLocalID = (int)file.getFilePointer();
            int pos = 8;
            msg.flags = BytePacket.readInt32(buf, pos);
            pos += 4;
            msg.timestamp = BytePacket.readInt32(buf, pos);
            pos += 4;


            msg.customerAppID = BytePacket.readInt64(buf, pos);
            pos += 8;
            msg.customerID = BytePacket.readInt64(buf, pos);
            pos += 8;
            msg.storeID = BytePacket.readInt64(buf, pos);
            pos += 8;
            msg.sellerID = BytePacket.readInt64(buf, pos);
            pos += 8;
            msg.isSupport = (int)(buf[pos]) == 1 ? true : false;
            pos += 1;

            if (msg.isSupport) {
                msg.sender = msg.storeID;
                msg.receiver = msg.customerID;
            } else {
                msg.sender = msg.customerID;
                msg.receiver = msg.storeID;
            }

            msg.setContent(new String(buf, pos, len - 41, "UTF-8"));
            return msg;
        } catch (Exception e) {
            Log.e("imservice", "read file exception:" + e);
            return null;
        }
    }

    public static boolean insertMessage(RandomAccessFile f, IMessage msg) throws IOException {
        long size = f.length();
        if (size < HEADER_SIZE && size > 0) {
            f.setLength(0);
            size = 0;
            Log.e("imservice", "truncate file");
        }
        if (size == 0) {
            writeHeader(f);
            size = HEADER_SIZE;
        }
        msg.msgLocalID = (int)size;
        f.seek(size);
        writeMessage(f, msg);
        return true;
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

    public boolean markMessageListened(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_LISTENED);
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
            return new CustomerMessageIterator(f);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public MessageIterator newMessageIterator(long uid, int firstMsgID) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "r");
            return new CustomerMessageIterator(f, firstMsgID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConversationIterator newConversationIterator() {
        return new CustomerConversationIterator(this.dir.listFiles(), Conversation.CONVERSATION_CUSTOMER_SERVICE);
    }
}
