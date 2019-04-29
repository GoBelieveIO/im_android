/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Text;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by houxh on 15/3/21.
 */
public class SQLGroupMessageDB  {
    private class GroupMessageIterator implements MessageIterator{
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


    private class ForwardGroupMessageIterator extends GroupMessageIterator {
        public ForwardGroupMessageIterator(SQLiteDatabase db, long group_id)  {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+group_id});
        }

        public ForwardGroupMessageIterator(SQLiteDatabase db, long group_id, int lastMsgID) {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? AND id < ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+group_id, ""+lastMsgID});
        }
    }

    private class BackwarkGroupMessageIterator extends GroupMessageIterator {
        public BackwarkGroupMessageIterator(SQLiteDatabase db, long group_id, int lastMsgID) {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? AND id > ? ORDER BY id";
            this.cursor = db.rawQuery(sql, new String[]{""+group_id, ""+lastMsgID});
        }
    }

    private class MiddleGroupMessageIterator extends GroupMessageIterator {
        public MiddleGroupMessageIterator(SQLiteDatabase db, long group_id, int lastMsgID) {
            String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? AND id > ? AND id < ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+group_id, ""+(lastMsgID-10), "" + (lastMsgID+10)});
        }
    }


    public class GroupConversationIterator implements ConversationIterator {
        private Cursor cursor;

        public GroupConversationIterator(SQLiteDatabase db) {
            this.cursor = db.rawQuery("SELECT MAX(id) as id, group_id FROM group_message GROUP BY group_id", null);
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
    private static final String FTS_TABLE_NAME = "group_message_fts";
    private static final String TAG = "beetle";

    private SQLiteDatabase db;

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getDb() {
        return this.db;
    }

    private boolean insertFTS(int msgLocalID, String text) {
        String t = tokenizer(text);
        ContentValues values = new ContentValues();
        values.put("docid", msgLocalID);
        values.put("content", t);
        db.insert("group_message_fts", null, values);
        return true;
    }


    public boolean insertMessage(IMessage msg, long gid) {
        ContentValues values = new ContentValues();
        values.put("sender", msg.sender);
        values.put("group_id", msg.receiver);
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
        msg.msgLocalID = (int)id;
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_TEXT) {
            Text text = (Text)msg.content;
            insertFTS((int)id, text.text);
        }
        return true;
    }

    public boolean insertMessages(List<IMessage> msgs) {
        db.beginTransaction();
        try {
            for (IMessage msg : msgs) {
                ContentValues values = new ContentValues();
                values.put("sender", msg.sender);
                values.put("group_id", msg.receiver);
                values.put("timestamp", msg.timestamp);
                values.put("flags", msg.flags);
                if (!TextUtils.isEmpty(msg.getUUID())) {
                    values.put("uuid", msg.getUUID());
                }
                values.put("content", msg.content.getRaw());


                long id = db.insert(TABLE_NAME, null, values);
                if (id == -1) {
                    return false;
                }
                msg.msgLocalID = (int) id;
                if (msg.content.getType() == MessageContent.MessageType.MESSAGE_TEXT) {
                    Text text = (Text) msg.content;
                    insertFTS((int) id, text.text);
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean updateContent(int msgLocalID, String content) {
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        int rows = db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgLocalID});
        return rows == 1;
    }


    public boolean acknowledgeMessage(int msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_ACK);
    }

    public boolean markMessageFailure(int msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_FAILURE);
    }

    public boolean markMessageListened(int msgLocalID) {
        return addFlag(msgLocalID,  MessageFlag.MESSAGE_FLAG_LISTENED);
    }

    public boolean eraseMessageFailure(int msgLocalID) {
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

    public boolean updateFlag(int msgLocalID, int flags) {
        ContentValues cv = new ContentValues();
        cv.put("flags", flags);
        db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgLocalID});
        return true;
    }

    public boolean removeMessage(int msgLocalID) {
        db.delete(TABLE_NAME, "id = ?", new String[]{""+msgLocalID});
        db.delete(FTS_TABLE_NAME, "rowid = ?", new String[]{""+msgLocalID});
        return true;
    }

    public boolean removeMessageIndex(int msgLocalID, long gid) {
        db.delete(FTS_TABLE_NAME, "rowid = ?", new String[]{""+msgLocalID});
        return true;
    }


    public boolean clearConversation(long gid) {
        db.delete(TABLE_NAME, "group_id = ?", new String[]{""+gid});
        return true;
    }

    public MessageIterator newMessageIterator(long gid) {
        return new ForwardGroupMessageIterator(db, gid);
    }

    public MessageIterator newForwardMessageIterator(long gid, int firstMsgID) {
        return new ForwardGroupMessageIterator(db, gid, firstMsgID);
    }

    public MessageIterator newBackwardMessageIterator(long gid, int msgID) {
        return new BackwarkGroupMessageIterator(db, gid, msgID);
    }

    public MessageIterator newMiddleMessageIterator(long gid, int msgID) {
        return new MiddleGroupMessageIterator(db, gid, msgID);
    }

    public ConversationIterator newConversationIterator() {
        return new GroupConversationIterator(db);
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
            messages.add(msg);
        }
        return messages;
    }

    private IMessage getMessage(Cursor cursor) {
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

    private IMessage getMessage(long id) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "content"},
                "id = ?", new String[]{""+id}, null, null, null);

        IMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    public IMessage getMessage(String uuid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "content"},
                "uuid = ?", new String[]{uuid}, null, null, null);


        IMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    public IMessage getLastMessage(long gid) {
        String sql = "SELECT id, sender, group_id, timestamp, flags, content FROM group_message WHERE group_id=? ORDER BY id DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{""+gid});
        boolean r = cursor.moveToNext();
        if (!r) {
            cursor.close();
            return null;
        }

        IMessage msg = getMessage(cursor);
        cursor.close();
        return msg;
    }

    public int getMessageId(String uuid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id"},
                "uuid = ?", new String[]{uuid}, null,null,null);
        boolean r = cursor.moveToNext();
        if (!r) {
            cursor.close();
            return 0;
        }

        int msgLocalId = cursor.getInt(cursor.getColumnIndex("id"));
        cursor.close();
        return msgLocalId;
    }
}
