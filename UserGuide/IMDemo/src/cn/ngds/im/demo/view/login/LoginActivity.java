package cn.ngds.im.demo.view.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.view.base.BaseActivity;
import cn.ngds.im.demo.view.chat.ChatActivity;
import cn.ngds.im.demo.view.header.HeaderFragment;

/**
 * LoginActivity
 * Description: 登录页面,给用户指定消息发送方Id
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private EditText mEtAccount;
    private EditText mEtTargetAccount;


    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (UserHelper.INSTANCE.validUser()) {
            go2Chat();
        }
        initHeaderFragment();
        Button btnLogin = (Button) findViewById(R.id.btn_login);
        mEtAccount = (EditText) findViewById(R.id.et_username);
        mEtTargetAccount = (EditText) findViewById(R.id.et_target_username);
        btnLogin.setOnClickListener(this);
        //如果是从聊天页面注销返回则进入此分支
        if (null != getIntent()) {
            long lastSenderId =
                getIntent().getLongExtra(ChatActivity.KEY_SENDER_ID, UserHelper.LOGOUT_ID);
            long lastReceiverId = getIntent()
                .getLongExtra(ChatActivity.KEY_RECEIVER_ID, UserHelper.LOGOUT_ID);
            if (UserHelper.LOGOUT_ID != lastSenderId) {
                mEtAccount.setText(String.valueOf(lastSenderId));
            }
            if (UserHelper.LOGOUT_ID != lastReceiverId) {
                mEtTargetAccount.setText(String.valueOf(lastReceiverId));
            }
        }
    }

    private void go2Chat() {
        Intent intent = new Intent();
        intent.setClass(this, ChatActivity.class);
        intent.putExtra(ChatActivity.KEY_SENDER_ID, UserHelper.INSTANCE.getSenderId());
        intent.putExtra(ChatActivity.KEY_RECEIVER_ID, UserHelper.INSTANCE.getReceiverId());
        startActivity(intent);
        finish();
    }

    private void initHeaderFragment() {
        HeaderFragment headerFragment =
            (HeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fg_header);

        headerFragment.setCenterText(R.string.login_login);
        headerFragment.showOrHideLeftButton(false);
        headerFragment.showOrHideRightButton(false);
    }

    @Override
    protected void bindView(Bundle savedInstanceState) {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_login) {
            if (mEtAccount.getText().toString().length() <= 0) {
                Toast.makeText(this, "请设置您的用户id", Toast.LENGTH_SHORT).show();
                return;
            }
            long senderId = Long.parseLong(mEtAccount.getText().toString());
            if (senderId == 0 || senderId == UserHelper.LOGOUT_ID) {
                Toast.makeText(this, "用户id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mEtTargetAccount.getText().toString().length() <= 0) {
                Toast.makeText(this, "请设置接收者的用户id", Toast.LENGTH_SHORT).show();
                return;
            }
            long receiverId = Long.parseLong(mEtTargetAccount.getText().toString());
            if (receiverId == 0 || receiverId == UserHelper.LOGOUT_ID) {
                Toast.makeText(this, "接收方id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }
            //设置用户id,进入MainActivity
            UserHelper.INSTANCE.setSenderId(senderId);
            UserHelper.INSTANCE.setReceiverId(receiverId);
            go2Chat();
        }
    }
}
