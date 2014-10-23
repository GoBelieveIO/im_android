package cn.ngds.im.demo.view.contacts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.db.UserDao;
import cn.ngds.im.demo.domain.User;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.view.base.TabFragment;
import cn.ngds.im.demo.view.chat.ChatActivity;
import cn.ngds.im.demo.view.header.HeaderFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * ContactsFragment
 * Description:
 */
public class ContactsFragment extends TabFragment
    implements HeaderFragment.HeaderButtonClickListener, AdapterView.OnItemLongClickListener,
    AdapterView.OnItemClickListener {
    private ListView mLvContacts;
    private ContactsListAdapter mContactsListAdapter;
    private List<User> mContactsList;
    private UserDao mUserDao;
    private HeaderFragment mHeaderFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mHeaderFragment =
            (HeaderFragment) getFragmentManager().findFragmentById(R.id.fg_header);
        mLvContacts = (ListView) view.findViewById(R.id.list);
        initHeaderFragment();
        initModel();
        return view;
    }

    private void initModel() {
        mUserDao = new UserDao(getActivity());
        mContactsList = new ArrayList<User>();
        mContactsList = mUserDao.getContactList();
        mContactsListAdapter = new ContactsListAdapter(getActivity(), mContactsList);
        mLvContacts.setAdapter(mContactsListAdapter);
        mContactsListAdapter.notifyDataSetChanged();
        mLvContacts.setOnItemLongClickListener(this);
        mLvContacts.setOnItemClickListener(this);
    }

    private void initHeaderFragment() {
        if (null == mHeaderFragment) {
            return;
        }
        mHeaderFragment.setCenterText(R.string.main_contacts);
        mHeaderFragment.showOrHideLeftButton(false);
        mHeaderFragment.showOrHideRightButton(true);
        mHeaderFragment.setRightButtonClickListener(this);
    }



    @Override
    public void onHeaderButtonClicked() {
        //产生添加好友对话框
        final EditText editText = new EditText(getActivity());
        editText.setHint("请输入玩家id");
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
            | InputType.TYPE_NUMBER_FLAG_SIGNED);
        editText.setSingleLine(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("添加好友").setIcon(
            R.drawable.ic_launcher).setView(editText).setNegativeButton(
            android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //添加好友
                    long userId = Long.parseLong(editText.getText().toString());
                    if (userId == UserHelper.INSTANCE.getUserId()) {
                        Toast.makeText(getActivity(), "不能和自己聊天...", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    User addedUser = new User();
                    addedUser.setUserId(userId);
                    mUserDao.saveContact(addedUser);
                    refreshList();
                }
            });
        builder.show();
    }

    @Override
    public void show() {
        initHeaderFragment();
    }

    @Override
    public void hide() {

    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position,
        long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("删除好友").setIcon(
            R.drawable.ic_launcher).setMessage("确认删除好友吗?").setNegativeButton(
            android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    long userId = ((User) parent.getItemAtPosition(position)).getUserId();
                    mUserDao.deleteContact(userId);
                    refreshList();
                }
            }).show();

        return false;
    }

    private void refreshList() {
        mContactsList = mUserDao.getContactList();
        mContactsListAdapter.setData(mContactsList);
        mContactsListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), ChatActivity.class);
        intent.putExtra(ChatActivity.KEY_USER_ID,
            mContactsListAdapter.getItem(position).getUserId());
        startActivity(intent);
    }
}
