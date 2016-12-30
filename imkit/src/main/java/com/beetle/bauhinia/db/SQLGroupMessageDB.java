package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Created by houxh on 15/3/21.
 */
public class SQLGroupMessageDB  {
    private class GroupMessageIterator implements MessageIterator{
        private Cursor cursor;

        public GroupMessageIterator(SQLiteDatabase db, long group_id)  {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+group_id});
        }

        public GroupMessageIterator(SQLiteDatabase db, long group_id, int lastMsgID) {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? AND id < ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+group_id, ""+lastMsgID});
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
            msg.receiver = cursor.getLong(cursor.getColumnIndex("group_id"));
            msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
            msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            msg.setContent(content);
            return msg;
        }
    }


    public class GroupConversationIterator implements ConversationIterator {

        private SQLiteDatabase db;
        private Cursor cursor;

        public GroupConversationIterator(SQLiteDatabase db) {
            this.db = db;
            this.cursor = db.rawQuery("SELECT MAX(id) as id, peer FROM group_message GROUP BY group_id", null);
        }


        private IMessage getMessage(long id) {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE id=?";
            Cursor cursor = db.rawQuery(sql, new String[]{""+id});

            IMessage msg = null;
            if (cursor.moveToNext()) {
                msg = new IMessage();
                msg.msgLocalID = cursor.getInt(cursor.getColumnIndex("id"));
                msg.sender = cursor.getLong(cursor.getColumnIndex("sender"));
                msg.receiver = cursor.getLong(cursor.getColumnIndex("group_id"));
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


    private static final String TABLE_NAME = "group_message";
    private static final String TAG = "beetle";

    private SQLiteDatabase db;

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getDb() {
        return this.db;
    }

    public boolean insertMessage(IMessage msg, long gid) {
        ContentValues values = new ContentValues();
        values.put("sender", msg.sender);
        values.put("group_id", msg.receiver);
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

    public boolean eraseMessageFailure(int msgLocalID, long gid) {
        int f = MessageFlag.MESSAGE_FLAG_FAILURE;
        return removeFlag(msgLocalID, f);
    }

    public boolean addFlag(int msgLocalID, int f) {
        String sql = "SELECT flags FROM group_message WHERE id=?";
        Cursor cursor = db.rawQuery(sql, new String[]{""+msgLocalID});
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

    public boolean removeFlag(int msgLocalID, int f) {
        String sql = "SELECT flags FROM group_message WHERE id=?";
        Cursor cursor = db.rawQuery(sql, new String[]{""+msgLocalID});
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

    public boolean removeMessage(int msgLocalID, long gid) {
        db.delete(TABLE_NAME, "id = ?", new String[]{""+msgLocalID});
        return true;
    }

    public boolean clearCoversation(long gid) {
        db.delete(TABLE_NAME, "group_id = ?", new String[]{""+gid});
        return true;
    }

    public MessageIterator newMessageIterator(long gid) {
        return new GroupMessageIterator(db, gid);
    }

    public MessageIterator newMessageIterator(long gid, int firstMsgID) {
        return new GroupMessageIterator(db, gid, firstMsgID);
    }

    public ConversationIterator newConversationIterator() {
        return new GroupConversationIterator(db);
    }

}
