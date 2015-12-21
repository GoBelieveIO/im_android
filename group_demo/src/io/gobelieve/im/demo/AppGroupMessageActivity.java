package io.gobelieve.im.demo;

import android.os.AsyncTask;

/**
 * Created by houxh on 15/8/8.
 */
public class AppGroupMessageActivity extends com.beetle.bauhinia.GroupMessageActivity {

    @Override
    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("name:%d", uid);
        return u;
    }

    @Override
    protected void asyncGetUser(long uid, GetUserCallback cb) {
        final long fuid = uid;
        final GetUserCallback fcb = cb;
        new AsyncTask<Void, Integer, User>() {
            @Override
            protected User doInBackground(Void... urls) {
                User u = new User();
                u.uid = fuid;
                u.name = String.format("name:%d", fuid);
                u.avatarURL = "";
                u.identifier = String.format("name:%d", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(User result) {
                fcb.onUser(result);
            }
        }.execute();
    }
}

