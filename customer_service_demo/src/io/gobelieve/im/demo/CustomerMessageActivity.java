package io.gobelieve.im.demo;

import android.os.AsyncTask;

import com.beetle.bauhinia.CustomerServiceMessageActivity;
import com.beetle.bauhinia.GroupMessageActivity;

/**
 * Created by houxh on 16/1/19.
 */
public class CustomerMessageActivity extends CustomerServiceMessageActivity {
    @Override
    protected CustomerServiceMessageActivity.User getUser(long uid) {
        CustomerServiceMessageActivity.User u = new CustomerServiceMessageActivity.User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("name:%d", uid);
        return u;
    }

    @Override
    protected void asyncGetUser(long uid, CustomerServiceMessageActivity.GetUserCallback cb) {
        final long fuid = uid;
        final CustomerServiceMessageActivity.GetUserCallback fcb = cb;
        new AsyncTask<Void, Integer, CustomerServiceMessageActivity.User>() {
            @Override
            protected CustomerServiceMessageActivity.User doInBackground(Void... urls) {
                CustomerServiceMessageActivity.User u = new CustomerServiceMessageActivity.User();
                u.uid = fuid;
                u.name = String.format("name:%d", fuid);
                u.avatarURL = "";
                u.identifier = String.format("name:%d", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(CustomerServiceMessageActivity.User result) {
                fcb.onUser(result);
            }
        }.execute();
    }
}
