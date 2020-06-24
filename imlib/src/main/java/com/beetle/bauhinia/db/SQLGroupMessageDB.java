package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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
            Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                    "group_id = ?", new String[]{""+group_id}, null, null, "id DESC");
            this.cursor = cursor;
        }

        public ForwardGroupMessageIterator(SQLiteDatabase db, long group_id, long lastMsgID) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                    "group_id=? AND id < ?", new String[]{""+group_id, ""+lastMsgID}, null, null, "id DESC");
            this.cursor = cursor;
        }
    }

    private class BackwarkGroupMessageIterator extends GroupMessageIterator {
        public BackwarkGroupMessageIterator(SQLiteDatabase db, long group_id, long lastMsgID) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                    "group_id=? AND id > ?", new String[]{""+group_id, ""+lastMsgID}, null, null, "id ASC");
            this.cursor = cursor;


        }
    }

    private class MiddleGroupMessageIterator extends GroupMessageIterator {
        public MiddleGroupMessageIterator(SQLiteDatabase db, long group_id, long lastMsgID) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                    "group_id=? AND id > ? AND id < ?", new String[]{""+group_id, ""+(lastMsgID-10), "" + (lastMsgID+10)}, null, null, "id DESC");
            this.cursor = cursor;
        }
    }

    private class TopicGroupMessageIterator extends GroupMessageIterator {
        public TopicGroupMessageIterator(SQLiteDatabase db, long group_id, String uuid) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                    "group_id=? AND reference = ?", new String[]{""+group_id, uuid}, null, null, "id DESC");
            this.cursor = cursor;
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
    private static final String READED_TABLE_NAME = "group_message_readed";
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

    boolean incrementReferenceCount(String uuid) {
        try {
            db.execSQL("UPDATE group_message SET reference_count=reference_count+1 WHERE uuid=?", new String[]{uuid});
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
        if (!TextUtils.isEmpty(msg.getReference())) {
            values.put("reference", msg.getReference());
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
        if (!TextUtils.isEmpty(msg.getReference())) {
            incrementReferenceCount(msg.getReference());
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
                if (!TextUtils.isEmpty(msg.getReference())) {
                    values.put("reference", msg.getReference());
                }
                values.put("content", msg.content.getRaw());

                long id = db.insert(TABLE_NAME, null, values);
                if (id == -1) {
                    return false;
                }
                msg.msgLocalID = id;
                if (msg.content.getType() == MessageContent.MessageType.MESSAGE_TEXT) {
                    Text text = (Text) msg.content;
                    insertFTS((int) id, text.text);
                }
                if (!TextUtils.isEmpty(msg.getReference())) {
                    incrementReferenceCount(msg.getReference());
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
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

    public boolean removeFlag(long msgLocalID, int f) {
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

    public boolean removeMessageIndex(long msgLocalID, long gid) {
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

    public MessageIterator newForwardMessageIterator(long gid, long firstMsgID) {
        return new ForwardGroupMessageIterator(db, gid, firstMsgID);
    }

    public MessageIterator newBackwardMessageIterator(long gid, long msgID) {
        return new BackwarkGroupMessageIterator(db, gid, msgID);
    }

    public MessageIterator newMiddleMessageIterator(long gid, long msgID) {
        return new MiddleGroupMessageIterator(db, gid, msgID);
    }

    public MessageIterator newTopicMessageIterator(long gid, String uuid) {
        return new TopicGroupMessageIterator(db, gid, uuid);
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
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    private IMessage getMessage(Cursor cursor) {
        IMessage msg = new IMessage();
        msg.msgLocalID = cursor.getLong(cursor.getColumnIndex("id"));
        msg.sender = cursor.getLong(cursor.getColumnIndex("sender"));
        msg.receiver = cursor.getLong(cursor.getColumnIndex("group_id"));
        msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
        msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
        msg.readerCount = cursor.getInt(cursor.getColumnIndex("reader_count"));
        msg.referenceCount = cursor.getInt(cursor.getColumnIndex("reference_count"));
        String content = cursor.getString(cursor.getColumnIndex("content"));
        msg.setContent(content);

        String tags = cursor.getString(cursor.getColumnIndex("tags"));
        if (!TextUtils.isEmpty(tags)) {
            String[] msgTags = tags.split(",");
            for (int i = 0; i < msgTags.length; i++) {
                msg.addTag(msgTags[i]);
            }
        }
        return msg;
    }

    public IMessage getMessage(long id) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                "id = ?", new String[]{""+id}, null, null, null);

        IMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    public IMessage getMessage(String uuid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                "uuid = ?", new String[]{uuid}, null, null, null);
        IMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    public IMessage getLastMessage(long gid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "sender", "group_id", "timestamp", "flags", "reader_count", "reference_count", "content", "tags"},
                "group_id = ?", new String[]{""+gid}, null, null, "id DESC");
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

    public boolean addMessageTag(long msgId, String tag) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"tags"},
                "id = ?", new String[]{"" + msgId}, null,null,null);
        if (cursor.moveToNext()) {
            String tags = cursor.getString(cursor.getColumnIndex("tags"));
            tags = tags != null ? tags : "";
            if (!tags.contains(tag)) {
                if (tags.length() > 0) {
                    tags += "," + tag;
                } else {
                    tags = tag;
                }
            }
            ContentValues cv = new ContentValues();
            cv.put("tags", tags);
            db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgId});
        }
        cursor.close();
        return true;
    }

    public boolean removeMessageTag(long msgId, String tag) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"tags"},
                "id = ?", new String[]{"" + msgId}, null,null,null);
        if (cursor.moveToNext()) {
            String tags = cursor.getString(cursor.getColumnIndex("tags"));
            tags = tags != null ? tags : "";
            if (tags.contains(tag)) {
                StringBuilder builder = new StringBuilder();
                String[] ts = tags.split(",");
                for (int i = 0; i < ts.length; i++) {
                    if (ts[i].equals(tag)) {
                        continue;
                    }
                    if (builder.length() > 0) {
                        builder.append(",");
                    }
                    builder.append(ts[i]);
                }
                tags = builder.toString();
            }
            ContentValues cv = new ContentValues();
            cv.put("tags", tags);
            db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgId});
        }
        cursor.close();
        return true;
    }


    public boolean addMessageReader(long msgId, long uid) {
        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put("msg_id", msgId);
            values.put("uid", uid);
            long id = db.insert(READED_TABLE_NAME, null, values);
            if (id == -1) {
                return false;
            }

            String sql = "SELECT COUNT(*) as count FROM group_message_readed WHERE msg_id=?";
            Cursor cursor = db.rawQuery(sql, new String[]{""+msgId});
            boolean r = cursor.moveToNext();
            if (!r) {
                cursor.close();
                return false;
            }
            int count = cursor.getInt(cursor.getColumnIndex("count"));
            cursor.close();

            ContentValues cv = new ContentValues();
            cv.put("reader_count", count);
            db.update(TABLE_NAME, cv, "id = ?", new String[]{""+msgId});

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public List<Long> getMessageReaders(long msgId) {
        Cursor cursor = db.query(READED_TABLE_NAME, new String[]{"uid"},
                "msg_id = ?", new String[]{"" + msgId}, null, null, null);
        ArrayList<Long> readers = new ArrayList<>();
        while(cursor.moveToNext()) {
            long uid = cursor.getLong(cursor.getColumnIndex("uid"));
            readers.add(uid);
        }
        cursor.close();
        return readers;
    }
}
