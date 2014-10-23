package cn.ngds.im.demo.view.contacts;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cn.ngds.im.demo.R;
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
        ViewHolder viewHolder;
        if (null == convertView) {
            convertView = mInflater.inflate(R.layout.item_contact, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.mTvUserId.setText(String.valueOf(getItem(position).getUserId()));

        return convertView;
    }


    class ViewHolder {
        private TextView mTvUserId;

        public ViewHolder(View view) {
            mTvUserId = (TextView) view.findViewById(R.id.tv_userid);
        }


    }
}
