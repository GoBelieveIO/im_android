package cn.ngds.im.demo.view.chat;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.base.IMDemoApplication;
import cn.ngds.im.demo.domain.NgdsMessage;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.receiver.NetworkStateReceiver;
import cn.ngds.im.demo.view.base.BaseActivity;
import cn.ngds.im.demo.view.header.HeaderFragment;
import cn.ngds.im.demo.view.login.LoginActivity;
import com.gameservice.sdk.im.IMMessage;
import com.gameservice.sdk.im.IMService;
import com.gameservice.sdk.im.IMServiceObserver;
import com.gameservice.sdk.push.api.IMsgReceiver;
import com.gameservice.sdk.push.api.SmartPush;
import com.gameservice.sdk.push.api.SmartPushOpenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity
 * Description: 用户聊天窗口
 */
public class ChatActivity extends BaseActivity
    implements IMServiceObserver, View.OnClickListener {
    private ListView mLvChatMsg;
    private MessageAdapter mMessageAdapter;
    private EditText mEtSendBoard;
    private Button mBtnSend;
    private long receiverId;
    private long senderId;
    public static final String KEY_SENDER_ID = "key_sender_id";
    public static final String KEY_RECEIVER_ID = "key_receiver_id";
    private static int msgLocalId = 1;
    private List<NgdsMessage> mChatMsgList;
    private IMService mIMService;
    private NetworkStateReceiver mNetworkStateReceiver;

    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_chat);
        if (getIntent() != null) {
            senderId = getIntent().getExtras().getLong(KEY_SENDER_ID);
            receiverId = getIntent().getExtras().getLong(KEY_RECEIVER_ID);
        }
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        initData();
        mEtSendBoard = (EditText) findViewById(R.id.et_send_message);
        mLvChatMsg = (ListView) findViewById(R.id.list);
        mMessageAdapter = new MessageAdapter(this, mChatMsgList);
        mLvChatMsg.setAdapter(mMessageAdapter);
        mBtnSend = (Button) findViewById(R.id.btn_send);
        mBtnSend.setOnClickListener(this);
        initHeaderView();
    }

    private void initData() {
        mNetworkStateReceiver = new NetworkStateReceiver();
        startPushService();
        startIMService();
        mChatMsgList = new ArrayList<NgdsMessage>();
        senderId = UserHelper.INSTANCE.getSenderId();

    }

    private void startIMService() {
        mIMService = IMService.getInstance();
        mIMService.setUID(UserHelper.INSTANCE.getSenderId());
        mIMService.addObserver(this);
    }

    private void initHeaderView() {
        HeaderFragment mHeaderFragment =
            (HeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fg_header);
        mHeaderFragment.setCenterText(
            getString(R.string.chat_activity_header, senderId,
                receiverId));
        mHeaderFragment.setLeftButtonClickListener(new HeaderFragment.HeaderButtonClickListener() {
            @Override
            public void onHeaderButtonClicked() {
                Intent loginIntent = new Intent();
                loginIntent.setClass(ChatActivity.this, LoginActivity.class);
                loginIntent.putExtra(KEY_SENDER_ID, senderId);
                loginIntent.putExtra(KEY_RECEIVER_ID, receiverId);
                startActivity(loginIntent);
                finish();
                IMDemoApplication.logout();
            }
        });

        mHeaderFragment.setRightButtonClickListener(new HeaderFragment.HeaderButtonClickListener() {
            @Override
            public void onHeaderButtonClicked() {
                mChatMsgList.clear();
                mMessageAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void bindView(Bundle savedInstanceState) {

    }

    private void startPushService() {
        // 注册消息接受者
        SmartPush.registerReceiver(new IMsgReceiver() {
            @Override
            public void onMessage(String message) {
                // message为透传消息，需开发者在此处理
                Log.i("PUSH", "透传消息:" + message);
                // 以下用于demo展现消息列表，开发者不用理会
                Intent intent = new Intent("cn.ngds.android.intent.MESSAGE");
                intent.putExtra("msg", message);
                sendBroadcast(intent);
            }

            @Override
            public void onDeviceToken(String deviceToken) {
                // SmartPushOpenUtils是 sdk提供本地化deviceToken的帮助类，开发者也可以自己实现本地化存储deviceToken
                SmartPushOpenUtils.saveDeviceToken(ChatActivity.this, deviceToken);
                // 玩家已登录
                // ***用于接收推送, 一定要调用该接口后才能接受推送
                SmartPush.bindDevice(ChatActivity.this, deviceToken, String.valueOf(senderId));
            }
        });
        // 注册服务，并启动服务
        SmartPush.registerService(this);
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {
        //TODO UI设计之后添加

    }

    @Override
    public void onPeerMessage(IMMessage msg) {
        if (null != mMessageAdapter && msg.receiver == senderId) {
            mChatMsgList.add(new NgdsMessage(msg, NgdsMessage.Direct.RECEIVE));
            mMessageAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onPeerMessageACK(int msgLocalID, long uid) {
        if (uid == receiverId && null != mMessageAdapter) {
            mMessageAdapter.onServerAck(msgLocalID, uid);
        }
    }

    @Override
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {
        //TODO UI设计之后添加
    }

    @Override
    public void onPeerMessageFailure(int msgLocalID, long uid) {
        if (uid == receiverId && null != mMessageAdapter) {
            mMessageAdapter.onSendFail(msgLocalID, uid);
        }
    }

    @Override
    public void onReset() {
        //异地登录,下线用户
        mIMService.stop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                //发送消息
                String msgContent = mEtSendBoard.getText().toString();
                if (TextUtils.isEmpty(msgContent)) {
                    Toast.makeText(this, "请输入发送内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                IMMessage msg = new IMMessage();
                msg.sender = senderId;
                msg.receiver = receiverId;
                msg.msgLocalID = msgLocalId++;
                msg.content = msgContent;
                NgdsMessage ngdsMessage = new NgdsMessage(msg, NgdsMessage.Direct.SEND);
                mChatMsgList.add(ngdsMessage);
                mMessageAdapter.notifyDataSetChanged();
                mEtSendBoard.setText("");
                mIMService.sendPeerMessage(msg);
                break;
        }
    }


    private IntentFilter mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

    @Override
    protected void onPause() {
        super.onPause();
        //应用离开前台后依靠push 接收离线消息
        unregisterReceiver(mNetworkStateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mNetworkStateReceiver, mIntentFilter);
        mIMService.start();
    }
}
