package cn.ngds.im.demo.view.contacts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import cn.ngds.im.demo.domain.User;
import cn.ngds.im.demo.view.adapter.SimpleListAdapter;

import java.util.List;

/**
 * ContactsListAdapter
 * Description:
 */
public class ContactsListAdapter extends SimpleListAdapter<User> {


    public ContactsListAdapter(Context context, List<User> data) {
        super(context, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
