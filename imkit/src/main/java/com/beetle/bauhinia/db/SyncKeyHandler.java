package com.beetle.bauhinia.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by houxh on 2016/11/2.
 */

public class SyncKeyHandler implements com.beetle.im.SyncKeyHandler {

    private String name;
    private Context context;

    private long syncKey;
    private HashMap<Long, Long> groupSyncKeys = new HashMap<Long, Long>();

    //preference file name
    public SyncKeyHandler(Context context, String name) {
        this.name = name;
        this.context = context;
    }

    public long getSyncKey() {
        return syncKey;
    }

    public HashMap<Long, Long> getSuperGroupSyncKeys() {
        return this.groupSyncKeys;
    }

    public boolean saveSyncKey(long syncKey) {
        this.syncKey = syncKey;
        return this.save();
    }

    public boolean saveGroupSyncKey(long groupID, long syncKey) {
        groupSyncKeys.put(groupID, syncKey);
        return this.save();
    }

    public void load() {
        SharedPreferences pref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        String s = pref.getString("sync_key", "");

        if (TextUtils.isEmpty(s)) {
            return;
        }
        try {
            JSONObject obj = new JSONObject(s);
            this.syncKey = obj.getLong("sync_key");

            this.groupSyncKeys = new HashMap<Long, Long>();
            JSONArray groups = obj.getJSONArray("groups");
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                groupSyncKeys.put(group.getLong("group_id"), group.getLong("sync_key"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean save() {
        SharedPreferences pref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        try {
            JSONArray groups = new JSONArray();
            for (HashMap.Entry<Long, Long> e : groupSyncKeys.entrySet()) {
                JSONObject t = new JSONObject();
                t.put("group_id", e.getKey());
                t.put("sync_key", e.getValue());
                groups.put(t);
            }

            JSONObject obj = new JSONObject();
            obj.put("groups", groups);
            obj.put("sync_key", syncKey);
            editor.putString("sync_key", obj.toString());
            editor.commit();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }
}
