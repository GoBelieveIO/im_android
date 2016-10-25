package io.gobelieve.im.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.beetle.bauhinia.CustomerManager;
import com.beetle.bauhinia.CustomerMessageActivity;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;


/**
  */
public class CustomerActivity extends FragmentActivity  implements
        NotificationCenter.NotificationCenterObserver{

    private static String TAG = "beetle";

    private EditText mEtAccount;
    private EditText mEtTargetAccount;


    private TextView newTextView;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEtAccount = (EditText) findViewById(R.id.et_username);
        mEtTargetAccount = (EditText) findViewById(R.id.et_target_username);

        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        CustomerManager.getInstance().init(getApplicationContext(),
                7, "sVDIlIiDUm7tWPYWhi6kfNbrqui3ez44", androidID);


        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.addObserver(this, CustomerMessageActivity.CLEAR_NEW_MESSAGES);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.removeObserver(this);
    }



    public void onChat(View v) {
        Log.i(TAG, "chat");
        CustomerManager.getInstance().startCustomerActivity(this, "客服");
    }

    public  void onLogout(View v) {
        CustomerManager.getInstance().unbindDeviceToken(new CustomerManager.OnUnbindCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "unbind device token success");
                CustomerManager.getInstance().unregisterClient();

                setContentView(R.layout.activity_login);
                mEtAccount = (EditText) findViewById(R.id.et_username);
                mEtTargetAccount = (EditText) findViewById(R.id.et_target_username);

                newTextView = null;
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i(TAG, "unbind device token failure");
            }
        });
    }


    private void onLoginSuccess() {
        CustomerManager.getInstance().login();

        setContentView(R.layout.activity_customer);
        newTextView = (TextView)findViewById(R.id.new_message);

        mEtAccount = null;
        mEtTargetAccount = null;

        //测试的devicetoken
        CustomerManager.getInstance().bindGCMDeviceToken("123dff", new CustomerManager.OnBindCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "bind device token success");
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i(TAG, "bind device token failure");
            }
        });

        CustomerManager.getInstance().getUnreadMessage(new CustomerManager.OnGetUnreadCallback() {
            @Override
            public void onSuccess(boolean hasUnread) {
                if (newTextView == null) {
                    return;
                }

                if (hasUnread) {
                    newTextView.setText("新消息");
                    newTextView.setTextColor(Color.RED);
                } else {
                    newTextView.setText("没有新消息");
                    newTextView.setTextColor(Color.BLACK);
                }
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i(TAG, "获取未读消息失败");
            }
        });
    }

    public void onLogin(View v) {
        if (mEtAccount.getText().toString().length() <= 0) {
            Toast.makeText(this, "请设置您的用户id", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mEtTargetAccount.getText().toString().length() <= 0) {
            Toast.makeText(this, "请设置您的用户名称", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        String uid = mEtAccount.getText().toString();
        String name = mEtTargetAccount.getText().toString();

        if (CustomerManager.getInstance().getClientID() > 0 &&
                CustomerManager.getInstance().getUid().equals(uid)) {
            onLoginSuccess();
        } else {
            CustomerManager.getInstance().registerClient(uid, name, "", new CustomerManager.OnRegisterCallback() {
                @Override
                public void onSuccess(long clientID) {
                    Log.i(TAG, "注册成功 client id:" + clientID);
                    CustomerActivity.this.onLoginSuccess();
                }

                @Override
                public void onFailure(int code, String message) {
                    Toast.makeText(CustomerActivity.this, "注册失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    public void onNotification(Notification notification) {
        if (notification.name.equals(CustomerMessageActivity.CLEAR_NEW_MESSAGES)) {
            if (newTextView != null) {
                newTextView.setText("没有新消息");
                newTextView.setTextColor(Color.BLACK);
            }
        }
    }

}
