/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ngds.im.demo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import cn.ngds.im.demo.domain.User;

import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public static final String TABLE_NAME = "contacts";
    public static final String COLUMN_ID = "user_id";

    private DbOpenHelper dbHelper;

    public UserDao(Context context) {
        dbHelper = DbOpenHelper.getInstance(context);
    }

    /**
     * 保存好友list
     *
     * @param contactList
     */
    public void saveContactList(List<User> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(TABLE_NAME, null, null);
            for (User user : contactList) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ID, user.getUserId());
                db.replace(TABLE_NAME, null, values);
            }
        }
    }

    /**
     * 获取好友列表
     *
     * @return
     */
    public List<User> getContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<User> userList = new ArrayList<User>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                long userId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                User user = new User();
                user.setUserId(userId);
                userList.add(user);
            }
            cursor.close();
        }
        return userList;
    }

    /**
     * 删除一个联系人
     *
     * @param userId
     */
    public void deleteContact(long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[] {String.valueOf(userId)});
        }
    }


    /**
     * 保存一个联系人
     *
     * @param user
     */
    public void saveContact(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, user.getUserId());
        if (db.isOpen()) {
            db.replace(TABLE_NAME, null, values);
        }
    }



}
