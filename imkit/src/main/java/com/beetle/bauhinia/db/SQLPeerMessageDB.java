package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Created by houxh on 14-7-22.
 */
public class SQLPeerMessageDB extends MessageDB {

    private class PeerMessageIterator implements MessageIterator{
        private Cursor cursor;

        public PeerMessageIterator(SQLiteDatabase db, long peer)  {
            String sql = "SELECT id, sender, receiver, timestamp, flags, content FROM peer_message WHERE peer = ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+peer});
        }

        public PeerMessageIterator(SQLiteDatabase db, long peer, int position)  {

            String sql = "SELECT id, sender, receiver, timestamp, flags, content FROM peer_message WHERE peer = ? AND id < ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+peer, ""+position});
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
            IMessage msg = new IMessage();
            msg.msgLocalID = cursor.getInt(cursor.getColumnIndex("id"));
            msg.sender = cursor.getLong(cursor.getColumnIndex("sender"));
            msg.receiver = cursor.getLong(cursor.getColumnIndex("receiver"));
            msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
            msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            msg.setContent(content);
            return msg;
        }
    }

    public class PeerConversationIterator implements ConversationIterator {
        private SQLiteDatabase db;
        private Cursor cursor;
        public PeerConversationIterator(SQLiteDatabase db) {
            this.db = db;
            this.cursor = db.rawQuery("SELECT MAX(id) as id, peer FROM peer_message GROUP BY peer", null);
        }

        private IMessage getMessage(long id) {
            String sql = "SELECT flags FROM peer_message WHERE id=?";
            Cursor cursor = db.rawQuery(sql, new String[]{""+id});

            IMessage msg = null;
            if (cursor.moveToNext()) {
                msg = new IMessage();
                msg.msgLocalID = cursor.getInt(cursor.getColumnIndex("id"));
                msg.sender = cursor.getLong(cursor.getColumnIndex("sender"));
                msg.receiver = cursor.getLong(cursor.getColumnIndex("receiver"));
                msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
                msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
                String content = cursor.getString(cursor.getColumnIndex("content"));
                msg.setContent(content);
            }
            cursor.close();
            return msg;

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


    private static final String TABLE_NAME = "peer_message";
    private static final String TAG = "beetle";


    private SQLiteDatabase db;

    public boolean insertMessage(IMessage msg, long uid) {
        ContentValues values = new ContentValues();
        values.put("peer", uid);
        values.put("sender", msg.sender);
        values.put("receiver", msg.receiver);
        values.put("timestamp", msg.timestamp);
        values.put("flags", msg.flags);
        values.put("content", msg.content.getRaw());
        long id = db.insert(TABLE_NAME, null, values);
        if (id == -1) {
            return  false;
        }
        msg.msgLocalID = (int)id;
        return true;
    }

    public boolean acknowledgeMessage(int msgLocalID, long uid) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_ACK);
    }

    public boolean markMessageFailure(int msgLocalID, long uid) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_FAILURE);
    }

    public boolean markMessageListened(int msgLocalID, long uid) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_LISTENED);
    }

    public boolean addFlag(int msgLocalID, int f) {
        String sql = "SELECT flags FROM peer_message WHERE id=?";
        Cursor cursor = db.rawQuery(sql, new String[]{""+msgLocalID});
        if (cursor.moveToNext()) {
            int flags = cursor.getInt(cursor.getColumnIndex("flags"));
            flags |= f;

            ContentValues cv = new ContentValues();
            cv.put("flags", flags);
            db.update(TABLE_NAME, cv, "flags = ?", new String[]{""+flags});
        }
        cursor.close();
        return true;
    }

    public boolean eraseMessageFailure(int msgLocalID, long uid) {
        String sql = "SELECT flags FROM peer_message WHERE id=?";
        Cursor cursor = db.rawQuery(sql, new String[]{""+msgLocalID});
        if (cursor.moveToNext()) {
            int flags = cursor.getInt(cursor.getColumnIndex("flags"));
            int f = MessageFlag.MESSAGE_FLAG_FAILURE;
            flags &= ~f;

            ContentValues cv = new ContentValues();
            cv.put("flags", flags);
            db.update(TABLE_NAME, cv, "flags = ?", new String[]{""+flags});
        }
        cursor.close();
        return true;
    }

    public boolean removeMessage(int msgLocalID, long uid) {
        db.delete(TABLE_NAME, "id = ?", new String[]{""+msgLocalID});
        return true;

    }

    public boolean clearCoversation(long uid) {
        db.delete(TABLE_NAME, "peer = ?", new String[]{""+uid});
        return true;
    }

    public MessageIterator newMessageIterator(long uid) {
        return new PeerMessageIterator(db, uid);
    }

    public MessageIterator newMessageIterator(long uid, int firstMsgID) {

        return new PeerMessageIterator(db, uid, firstMsgID);

    }

    public ConversationIterator newConversationIterator() {
        return new PeerConversationIterator(db);
    }
}
