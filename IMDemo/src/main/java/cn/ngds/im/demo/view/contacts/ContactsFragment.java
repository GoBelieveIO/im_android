package cn.ngds.im.demo.view.contacts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.db.UserDao;
import cn.ngds.im.demo.domain.User;
import cn.ngds.im.demo.view.base.TabFragment;
import cn.ngds.im.demo.view.header.HeaderFragment;

/**
 * ContactsFragment
 * Description:
 */
public class ContactsFragment extends TabFragment
    implements HeaderFragment.HeaderButtonClickListener {
    private ListView mLvContacts;
    private UserDao mUserDao;
    private HeaderFragment mHeaderFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mHeaderFragment =
            (HeaderFragment) getFragmentManager().findFragmentById(R.id.fg_header);
        initHeaderFragment();
        initModel();
        mLvContacts = (ListView) view.findViewById(R.id.list);
        return view;
    }

    private void initModel() {
        mUserDao = new UserDao(getActivity());
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
                    User addedUser = new User();
                    addedUser.setUserId(userId);
                    mUserDao.saveContact(addedUser);
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
}
