package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Text;

import java.util.ArrayList;

/**
 * Created by houxh on 16/1/18.
 */
public class SQLCustomerMessageDB {

    private class CustomerMessageIterator implements MessageIterator{
        private Cursor cursor;

        public CustomerMessageIterator(SQLiteDatabase db, long storeID) {
            String sql = "SELECT  id, customer_id, customer_appid, store_id, seller_id, timestamp, flags, is_support, content FROM customer_message WHERE store_id = ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+storeID});
        }

        public CustomerMessageIterator(SQLiteDatabase db, long storeID, long lastMsgID) {
            String sql = "SELECT  id, customer_id, customer_appid, store_id, seller_id, timestamp, flags, is_support, content FROM customer_message WHERE store_id = ? AND id < ? ORDER BY id DESC";
            this.cursor = db.rawQuery(sql, new String[]{""+storeID, ""+lastMsgID});
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
            ICustomerMessage msg = getMessage(cursor);
            return msg;
        }
    }

    public class CustomerConversationIterator implements ConversationIterator {
        private SQLiteDatabase db;
        private Cursor cursor;

        public CustomerConversationIterator(SQLiteDatabase db) {
            this.db = db;
            this.cursor = db.rawQuery("SELECT MAX(id) as id, store_id FROM customer_message GROUP BY store_id", null);
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


    private static final String TABLE_NAME = "customer_message";
    private static final String FTS_TABLE_NAME = "customer_message_fts";
    private static final String TAG = "beetle";

    private SQLiteDatabase db;

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getDb() {
        return this.db;
    }

    private boolean insertFTS(long msgLocalID, String text) {
        String t = tokenizer(text);
        ContentValues values = new ContentValues();
        values.put("docid", msgLocalID);
        values.put("content", t);
        db.insert("customer_message_fts", null, values);
        return true;
    }


    public boolean insertMessage(IMessage m) {
        ICustomerMessage msg = (ICustomerMessage)m;
        ContentValues values = new ContentValues();
        values.put("customer_id", msg.customerID);
        values.put("customer_appid", msg.customerAppID);
        values.put("store_id", msg.storeID);
        values.put("seller_id", msg.sellerID);
        values.put("timestamp", msg.timestamp);
        values.put("flags", msg.flags);
        values.put("is_support", msg.isSupport ? 1 : 0);
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

    public boolean eraseMessageFailure(long msgLocalID) {
        int f = MessageFlag.MESSAGE_FLAG_FAILURE;
        return removeFlag(msgLocalID, f);
    }

    public boolean addFlag(long msgLocalID, int f) {
        String sql = "SELECT flags FROM customer_message WHERE id=?";
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
        String sql = "SELECT flags FROM customer_message WHERE id=?";
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

    public boolean removeMessageIndex(long msgLocalID) {
        db.delete(FTS_TABLE_NAME, "rowid = ?", new String[]{""+msgLocalID});
        return true;
    }


    public boolean clearConversation(long storeID) {
        db.delete(TABLE_NAME, "store_id = ?", new String[]{""+storeID});
        return true;
    }

    public MessageIterator newMessageIterator(long storeID) {
        return new CustomerMessageIterator(db, storeID);
    }

    public MessageIterator newForwardMessageIterator(long storeID, long firstMsgID) {
        return new CustomerMessageIterator(db, storeID, firstMsgID);
    }

    public MessageIterator newBackwardMessageIterator(long storeID, long msgID) {
        return null;
    }

    public MessageIterator newMiddleMessageIterator(long storeID, long msgID) {
        return null;
    }

    public ConversationIterator newConversationIterator() {
        return new CustomerConversationIterator(db);
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


    private ICustomerMessage getMessage(Cursor cursor) {
        ICustomerMessage msg = new ICustomerMessage();
        msg.msgLocalID = cursor.getLong(cursor.getColumnIndex("id"));
        msg.customerID = cursor.getLong(cursor.getColumnIndex("customer_id"));
        msg.customerAppID = cursor.getLong(cursor.getColumnIndex("customer_appid"));
        msg.storeID = cursor.getLong(cursor.getColumnIndex("store_id"));
        msg.sellerID = cursor.getLong(cursor.getColumnIndex("seller_id"));
        msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
        msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
        msg.isSupport = cursor.getInt(cursor.getColumnIndex("is_support")) == 1;
        String content = cursor.getString(cursor.getColumnIndex("content"));
        msg.setContent(content);
        return msg;
    }

    public ICustomerMessage getMessage(long id) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "customer_id", "customer_appid", "store_id", "seller_id", "timestamp", "flags", "is_support", "content"},
                "id = ?", new String[]{""+id}, null, null, null);
        ICustomerMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
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

    public ICustomerMessage getMessage(String uuid) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "customer_id", "customer_appid", "store_id", "seller_id", "timestamp", "flags", "is_support", "content"},
                "uuid = ?", new String[]{uuid}, null, null, null);
        ICustomerMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    public ICustomerMessage getLastMessage(long storeID) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "customer_id", "customer_appid", "store_id", "seller_id", "timestamp", "flags", "is_support", "content"},
                "store_id = ?", new String[]{""+storeID}, null, null, "id DESC");
        ICustomerMessage msg = null;
        if (cursor.moveToNext()) {
            msg = getMessage(cursor);
        }
        cursor.close();
        return msg;

    }

}
