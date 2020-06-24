package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Text;

import java.util.ArrayList;

/**
 * Created by houxh on 14-7-22.
 */
public class SQLPeerMessageDB {

    private class PeerMessageIterator implements MessageIterator{
        protected Cursor cursor;

        public IMessage next() {
            if (cursor == null) {
                return null;
            }
            boolean r = cursor.moveToNext();
            if (!r) {
                cursor.close();
                cursor = null;
                return null;
            }
            IMessage msg = getMessage(cursor);
            return msg;
        }
    }

    private class ForwardPeerMessageInterator extends PeerMessageIterator {
        public ForwardPeerMessageInterator(SQLiteDatabase db, long peer)  {

            this.cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                    "peer = ? AND secret= ?", new String[]{""+peer, ""+secret}, null, null, "id DESC");
        }

        public ForwardPeerMessageInterator(SQLiteDatabase db, long peer, long position)  {
            this.cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                    "peer = ? AND secret = ? AND id < ?", new String[]{""+peer, ""+secret, ""+position},
                    null, null, "id DESC");
        }
    }

    private class BackwardPeerMessageInterator extends PeerMessageIterator {
        public BackwardPeerMessageInterator(SQLiteDatabase db, long peer, long position)  {
            this.cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                    "peer = ? AND secret=? AND id > ?", new String[]{""+peer, ""+secret, ""+position},
                    null, null, "id");
        }
    }

    private class MiddlePeerMessageInterator extends PeerMessageIterator {
        public MiddlePeerMessageInterator(SQLiteDatabase db, long peer, long position)  {
            this.cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                    "peer = ? AND secret=? AND id > ? AND id < ?",
                    new String[]{""+peer, ""+secret, ""+(position - 10), "" + (position + 10)},
                    null, null, "id DESC");
        }
    }

    public class PeerConversationIterator implements ConversationIterator {
        private Cursor cursor;
        public PeerConversationIterator(SQLiteDatabase db) {
            this.cursor = db.query(TABLE_NAME, new String[]{"MAX(id) as id", "peer"},
                    null, null, "peer", null, null);
        }

        public IMessage next() {
            if (cursor == null) {
                return null;
            }
            boolean r = cursor.moveToNext();
            if (!r) {
                cursor.close();
                cursor = null;
                return null;
            }
            long id = cursor.getLong(cursor.getColumnIndex("id"));
            IMessage msg = getMessage(id);
            return msg;
        }
    }

    static protected final String TABLE_NAME = "peer_message";
    static protected final String FTS_TABLE_NAME = "peer_message_fts";

    protected int secret;

    private SQLiteDatabase db;

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getDb() {
        return this.db;
    }


    public boolean insertMessage(IMessage msg, long uid) {
        ContentValues values = new ContentValues();
        values.put("peer", uid);
        values.put("sender", msg.sender);
        values.put("receiver", msg.receiver);
        values.put("secret", secret);
        values.put("timestamp", msg.timestamp);
        values.put("flags", msg.flags);
        if (!TextUtils.isEmpty(msg.getUUID())) {
            values.put("uuid", msg.getUUID());
        }
        values.put("content", msg.content.getRaw());
        long id = db.insert(TABLE_NAME, null, values);
        if (id == -1) {
            return  false;
        }
        msg.msgLocalID = id;

        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_TEXT) {
            Text text = (Text)msg.content;
            insertFTS((int)id, text.text);
        }
        return true;
    }

    public boolean updateContent(long msgLocalID, String content) {
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        int rows = db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgLocalID});
        return rows == 1;
    }

    public boolean acknowledgeMessage(long msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_ACK);
    }

    public boolean markMessageFailure(long msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_FAILURE);
    }

    public boolean markMessageListened(long msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_LISTENED);
    }

    public boolean markMessageReaded(long msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_READED);
    }

    public boolean eraseMessageFailure(long msgLocalID) {
        int f = MessageFlag.MESSAGE_FLAG_FAILURE;
        return removeFlag(msgLocalID, f);
    }

    public boolean addFlag(long msgLocalID, int f) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"flags"},
                "id=?", new String[]{""+msgLocalID},
                null, null, null);
        if (cursor.moveToNext()) {
            int flags = cursor.getInt(cursor.getColumnIndex("flags"));
            flags |= f;

            ContentValues cv = new ContentValues();
            cv.put("flags", flags);
            db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgLocalID});
        }
        cursor.close();
        return true;
    }

    public boolean removeFlag(long msgLocalID, int f) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"flags"},
                "id=?", new String[]{""+msgLocalID},
                null, null, null);
        if (cursor.moveToNext()) {
            int flags = cursor.getInt(cursor.getColumnIndex("flags"));
            flags &= ~f;

            ContentValues cv = new ContentValues();
            cv.put("flags", flags);
            db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgLocalID});
        }
        cursor.close();
        return true;
    }

    public boolean updateFlag(long msgLocalID, int flags) {
        ContentValues cv = new ContentValues();
        cv.put("flags", flags);
        db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgLocalID});
        return true;
    }


    public boolean removeMessage(long msgLocalID) {
        db.delete(TABLE_NAME, "id = ?", new String[]{""+msgLocalID});
        db.delete(FTS_TABLE_NAME, "rowid = ?", new String[]{""+msgLocalID});
        return true;
    }

    public boolean removeMessageIndex(long msgLocalID) {
        db.delete(FTS_TABLE_NAME, "rowid = ?", new String[]{""+msgLocalID});
        return true;
    }

    public boolean clearConversation(long uid) {
        db.delete(TABLE_NAME, "peer = ? AND secret= ?", new String[]{""+uid, "" + secret});
        return true;
    }

    //获取最近的消息
    public MessageIterator newMessageIterator(long uid) {
        return new ForwardPeerMessageInterator(db, uid);
    }

    //获取之前的消息
    public MessageIterator newForwardMessageIterator(long uid, long firstMsgID) {
        return new ForwardPeerMessageInterator(db, uid, firstMsgID);
    }

    //获取之后的消息
    public MessageIterator newBackwardMessageIterator(long uid, long msgID) {
        return new BackwardPeerMessageInterator(db, uid, msgID);
    }

    //获取前后的消息
    public MessageIterator newMiddleMessageIterator(long uid, long msgID) {
        return new MiddlePeerMessageInterator(db, uid, msgID);
    }

    public ConversationIterator newConversationIterator() {
        return new PeerConversationIterator(db);
    }

    private String tokenizer(String key) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            builder.append(c);
            if (c >= 0x4e00 && c <= 0x9fff) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    public ArrayList<IMessage> search(String key) {
        key = key.replace("'", "\'");
        String k = this.tokenizer(key);
        Cursor cursor = db.query(FTS_TABLE_NAME, new String[]{"rowid"},
                "content MATCH(?)",  new String[]{k},
                null, null, null);

        ArrayList<Long> rows = new ArrayList<Long>();
        while(cursor.moveToNext()) {
            long rowid = cursor.getInt(cursor.getColumnIndex("rowid"));
            rows.add(rowid);
        }
        cursor.close();

        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        for (int i = 0; i < rows.size(); i++) {
            IMessage msg = getMessage(rows.get(i));
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    private boolean insertFTS(int msgLocalID, String text) {
        String t = tokenizer(text);
        ContentValues values = new ContentValues();
        values.put("docid", msgLocalID);
        values.put("content", t);
        db.insert(FTS_TABLE_NAME, null, values);
        return true;
    }

    private IMessage getMessage(Cursor cursor) {
        IMessage msg = new IMessage();
        msg.msgLocalID = cursor.getLong(cursor.getColumnIndex("id"));
        msg.sender = cursor.getLong(cursor.getColumnIndex("sender"));
        msg.receiver = cursor.getLong(cursor.getColumnIndex("receiver"));
        msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
        msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
        String content = cursor.getString(cursor.getColumnIndex("content"));
        msg.secret = cursor.getInt(cursor.getColumnIndex("secret")) == 1;
        msg.setContent(content);
        return msg;
    }

    public IMessage getMessage(long id) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                "id = ?", new String[]{""+id}, null, null, null);

        IMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    public IMessage getMessage(String uuid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                "uuid = ?", new String[]{uuid}, null, null, null);

        IMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    //获取到最新的消息
    public IMessage getLastMessage(long peer) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "receiver", "timestamp", "flags", "content", "secret"},
                "peer = ? AND secret= ?", new String[]{""+peer, ""+secret}, null, null, "id DESC");
        boolean r = cursor.moveToNext();
        if (!r) {
            cursor.close();
            return null;
        }

        IMessage msg = getMessage(cursor);
        cursor.close();
        return msg;
    }

    public long getMessageId(String uuid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id"},
                "uuid = ?", new String[]{uuid}, null,null,null);
        boolean r = cursor.moveToNext();
        if (!r) {
            cursor.close();
            return 0;
        }

        long msgLocalId = cursor.getLong(cursor.getColumnIndex("id"));
        cursor.close();
        return msgLocalId;
    }
}
