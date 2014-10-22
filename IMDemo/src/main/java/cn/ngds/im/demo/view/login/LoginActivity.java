package cn.ngds.im.demo.view.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import cn.ngds.im.demo.MainActivity;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.view.base.BaseActivity;
import cn.ngds.im.demo.view.header.HeaderFragment;

/**
 * LoginActivity
 * Description: 登录页面,给用户指定消息发送方Id
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private EditText mEtAccount;


    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (UserHelper.INSTANCE.validUser()) {
            go2Main();
        }
        initHeaderFragment();
        Button btnLogin = (Button) findViewById(R.id.btn_login);
        mEtAccount = (EditText) findViewById(R.id.et_username);
        btnLogin.setOnClickListener(this);
    }

    private void go2Main() {
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
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
            long userId = Long.parseLong(mEtAccount.getText().toString());
            if (userId == 0 || userId == UserHelper.LOGOUT_ID) {
                Toast.makeText(this, "用户id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }
            //设置用户id,进入MainActivity
            UserHelper.INSTANCE.setUserId(userId);
            go2Main();
        }
    }
}
