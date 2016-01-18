package io.gobelieve.im.demo;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.CustomerServiceMessageActivity;
import com.beetle.bauhinia.PeerMessageActivity;


public class MessageListActivity extends com.beetle.bauhinia.MessageListActivity {
    private static final String TAG = "beetle";

    @Override
    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("%d", uid);
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
                u.name = String.format("%d", fuid);
                u.avatarURL = "";
                u.identifier = String.format("%d", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(User result) {
                fcb.onUser(result);
            }
        }.execute();
    }

    @Override
    protected Group getGroup(long gid) {
        Group g = new Group();
        g.gid = gid;
        g.name = null;
        g.avatarURL = "";
        g.identifier = String.format("%d", gid);
        return g;
    }

    @Override
    protected void asyncGetGroup(long gid, GetGroupCallback cb) {
        final long fgid = gid;
        final GetGroupCallback fcb = cb;
        new AsyncTask<Void, Integer, Group>() {
            @Override
            protected Group doInBackground(Void... urls) {
                Group g = new Group();
                g.gid = fgid;
                g.name = String.format("%d", fgid);
                g.avatarURL = "";
                g.identifier = String.format("%d", fgid);
                return g;
            }
            @Override
            protected void onPostExecute(Group result) {
                fcb.onGroup(result);
            }
        }.execute();
    }

    @Override
    protected void onPeerClick(long uid) {
        User u = getUser(uid);

        Intent intent = new Intent(this, PeerMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", uid);
        if (TextUtils.isEmpty(u.name)) {
            intent.putExtra("peer_name", u.identifier);
        } else {
            intent.putExtra("peer_name", u.name);
        }
        intent.putExtra("current_uid", this.currentUID);
        startActivity(intent);
    }

    @Override
    protected void onGroupClick(long gid) {
        Log.i(TAG, "group conversation");
    }

    @Override
    protected void onCustomerServiceClick(long id) {
        Intent intent = new Intent(this, CustomerServiceMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", 0);
        intent.putExtra("peer_name", "客服");
        intent.putExtra("current_uid", this.currentUID);
        startActivity(intent);
    }
}
