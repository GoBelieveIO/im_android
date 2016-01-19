package io.gobelieve.im.demo;

import android.os.AsyncTask;

/**
 * Created by houxh on 16/1/19.
 */
public class CustomerMessageActivity extends com.beetle.bauhinia.CustomerMessageActivity {
    @Override
    protected com.beetle.bauhinia.CustomerMessageActivity.User getUser(long uid) {
        com.beetle.bauhinia.CustomerMessageActivity.User u = new com.beetle.bauhinia.CustomerMessageActivity.User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("name:%d", uid);
        return u;
    }

    @Override
    protected void asyncGetUser(long uid, com.beetle.bauhinia.CustomerMessageActivity.GetUserCallback cb) {
        final long fuid = uid;
        final com.beetle.bauhinia.CustomerMessageActivity.GetUserCallback fcb = cb;
        new AsyncTask<Void, Integer, com.beetle.bauhinia.CustomerMessageActivity.User>() {
            @Override
            protected com.beetle.bauhinia.CustomerMessageActivity.User doInBackground(Void... urls) {
                com.beetle.bauhinia.CustomerMessageActivity.User u = new com.beetle.bauhinia.CustomerMessageActivity.User();
                u.uid = fuid;
                u.name = String.format("name:%d", fuid);
                u.avatarURL = "";
                u.identifier = String.format("name:%d", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(com.beetle.bauhinia.CustomerMessageActivity.User result) {
                fcb.onUser(result);
            }
        }.execute();
    }
}
