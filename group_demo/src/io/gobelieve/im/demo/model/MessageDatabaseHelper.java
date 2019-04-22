/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MessageDatabaseHelper {
    private static final String TAG = "goubuli";

    private static final int DATABASE_VERSION  = 3;

    private static final Object lock = new Object();

    private static MessageDatabaseHelper instance;
    private DatabaseHelper databaseHelper;


    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDatabase(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "upgrade database, old version:" + oldVersion + " new version:" + newVersion);
            assert(newVersion == 3);
        }

        private void createDatabase(SQLiteDatabase db) {
            db.execSQL(SQLCreator.PEER_MESSAGE);
            db.execSQL(SQLCreator.GROUP_MESSAGE);
            db.execSQL(SQLCreator.CUSTOMER_MESSAGE);
            db.execSQL(SQLCreator.PEER_MESSAGE_FTS);
            db.execSQL(SQLCreator.GROUP_MESSAGE_FTS);
            db.execSQL(SQLCreator.CUSTOMER_MESSAGE_FTS);
            db.execSQL(SQLCreator.PEER_MESSAGE_IDX);
            db.execSQL(SQLCreator.PEER_MESSAGE_UUID_IDX);
            db.execSQL(SQLCreator.GROUP_MESSAGE_UUID_IDX);
            db.execSQL(SQLCreator.CUSTOMER_MESSAGE_UUID_IDX);
            db.execSQL(SQLCreator.CONVERSATION);
            db.execSQL(SQLCreator.CONVERSATION_IDX);
        }
    }

    public static MessageDatabaseHelper getInstance() {
        synchronized (lock) {
            if (instance == null)
                instance = new MessageDatabaseHelper();
            return instance;
        }
    }

    private MessageDatabaseHelper() {

    }

    public void open(Context context, String name) {
        if (this.databaseHelper != null) {
            this.databaseHelper.close();
        }
        this.databaseHelper = new DatabaseHelper(context, name, null, DATABASE_VERSION);
    }

    public SQLiteDatabase getDatabase() {
        return this.databaseHelper.getWritableDatabase();
    }

    public void close() {
        if (this.databaseHelper != null) {
            this.databaseHelper.close();
            this.databaseHelper = null;
        }
    }
}
