package com.beetle.bauhinia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

        public CustomerMessageIterator(SQLiteDatabase db, long storeID, int lastMsgID) {
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
            ICustomerMessage msg = new ICustomerMessage();
            msg.msgLocalID = cursor.getInt(cursor.getColumnIndex("id"));
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
    }

    public class CustomerConversationIterator implements ConversationIterator {
        private SQLiteDatabase db;
        private Cursor cursor;

        public CustomerConversationIterator(SQLiteDatabase db) {
            this.db = db;
            this.cursor = db.rawQuery("SELECT MAX(id) as id, store_id FROM customer_message GROUP BY store_id", null);
        }


        private IMessage getMessage(long id) {
            String sql = "SELECT  id, customer_id, customer_appid, store_id, seller_id, timestamp, flags, is_support, content  FROM group_message WHERE id=?";
            Cursor cursor = db.rawQuery(sql, new String[]{""+id});

            ICustomerMessage msg = null;
            if (cursor.moveToNext()) {
                msg = new ICustomerMessage();
                msg.msgLocalID = cursor.getInt(cursor.getColumnIndex("id"));
                msg.customerID = cursor.getLong(cursor.getColumnIndex("customer_id"));
                msg.customerAppID = cursor.getLong(cursor.getColumnIndex("customer_appid"));
                msg.storeID = cursor.getLong(cursor.getColumnIndex("store_id"));
                msg.sellerID = cursor.getLong(cursor.getColumnIndex("seller_id"));
                msg.timestamp = cursor.getInt(cursor.getColumnIndex("timestamp"));
                msg.flags = cursor.getInt(cursor.getColumnIndex("flags"));
                msg.isSupport = cursor.getInt(cursor.getColumnIndex("is_support")) == 1;
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


    private static final String TABLE_NAME = "customer_message";
    private static final String TAG = "beetle";

    private SQLiteDatabase db;

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public SQLiteDatabase getDb() {
        return this.db;
    }

    public boolean insertMessage(IMessage m, long uid) {
        ICustomerMessage msg = (ICustomerMessage)m;
        ContentValues values = new ContentValues();
        values.put("customer_id", msg.customerID);
        values.put("customer_appid", msg.customerAppID);
        values.put("store_id", msg.storeID);
        values.put("seller_id", msg.sellerID);
        values.put("timestamp", msg.timestamp);
        values.put("flags", msg.flags);
        values.put("is_support", msg.isSupport ? 1 : 0);
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


    public boolean removeMessage(int msgLocalID, long uid) {
        db.delete(TABLE_NAME, "id = ?", new String[]{""+msgLocalID});
        return true;
    }

    public boolean clearCoversation(long storeID) {
        db.delete(TABLE_NAME, "store_id = ?", new String[]{""+storeID});
        return true;
    }

    public MessageIterator newMessageIterator(long storeID) {
        return new CustomerMessageIterator(db, storeID);
    }

    public MessageIterator newMessageIterator(long storeID, int firstMsgID) {
        return new CustomerMessageIterator(db, storeID, firstMsgID);
    }

    public ConversationIterator newConversationIterator() {
        return new CustomerConversationIterator(db);
    }
}
