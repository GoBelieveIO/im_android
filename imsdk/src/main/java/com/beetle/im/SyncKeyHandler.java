package com.beetle.im;

/**
 * Created by houxh on 2016/11/2.
 */

public interface SyncKeyHandler {
    boolean saveSyncKey(long syncKey);
    boolean saveGroupSyncKey(long groupID, long syncKey);
}
