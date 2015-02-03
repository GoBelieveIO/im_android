package cn.ngds.im.demo.view.chat;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
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
import com.gameservice.sdk.analystic.analytics.AnalysticAgent;
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
    implements View.OnClickListener {
    private ListView mLvChatMsg;
    private MessageAdapter mMessageAdapter;
    private EditText mEtSendBoard;
    private Button mBtnSend;
    private long receiverId;
    private long senderId;
    private String token;
    public static final String KEY_SENDER_ID = "key_sender_id";
    public static final String KEY_RECEIVER_ID = "key_receiver_id";
    public static final String KEY_TOKEN_ID = "key_token_id";

    private static int msgLocalId = 1;
    private List<NgdsMessage> mChatMsgList;
    private IMService mIMService;
    private NetworkStateReceiver mNetworkStateReceiver;
    private HeaderFragment mHeaderFragment;

    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_chat_tmp);
        if (getIntent() != null) {
            senderId = getIntent().getExtras().getLong(KEY_SENDER_ID);
            receiverId = getIntent().getExtras().getLong(KEY_RECEIVER_ID);
            token = getIntent().getExtras().getString(KEY_TOKEN_ID);
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
        //获取IMService
        mIMService = IMService.getInstance();
        mIMService.setAccessToken(this.token);
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mIMService.setDeviceID(androidID);

        //注册接受消息状态以及送达回调的观察者
        mIMService.addObserver(new IMServiceObserver() {
            /**
             * 连接状态改变
             * @param state
             */
            @Override
            public void onConnectState(IMService.ConnectState state) {
                if (null == mHeaderFragment || null == state) {
                    return;
                }
                String status = null;
                switch (state) {
                    case STATE_CONNECTING:
                        status = "连接中...";
                        break;
                    case STATE_CONNECTED:
                        status = getString(R.string.chat_activity_header, senderId,
                            receiverId);
                        break;
                    case STATE_CONNECTFAIL:
                        status = "连接失败";
                        break;
                    case STATE_UNCONNECTED:
                        status = "未连接";
                        break;
                }
                mHeaderFragment.setCenterText(status);
            }

            /**
             * 收到IM消息
             *
             * @param msg 消息
             */
            @Override
            public void onPeerMessage(IMMessage msg) {
                if (null != mMessageAdapter && msg.receiver == senderId) {
                    if (msg.sender != receiverId) {
                        msg.content = msg.sender + " : " + msg.content;
                    }
                    mChatMsgList.add(new NgdsMessage(msg, NgdsMessage.Direct.RECEIVE));
                    mMessageAdapter.notifyDataSetChanged();
                }
            }

            /**
             * 服务器已收到发送消息回调
             *
             * @param msgLocalID 消息本地id
             * @param uid        发送方id
             */

            @Override
            public void onPeerMessageACK(int msgLocalID, long uid) {
                if (uid == receiverId && null != mMessageAdapter) {
                    mMessageAdapter.onServerAck(msgLocalID);
                }
            }

            /**
             * 接收方已收到
             *
             * @param msgLocalID 消息本地id
             * @param uid        发送方id
             */
            @Override
            public void onPeerMessageRemoteACK(int msgLocalID, long uid) {
                if (uid == receiverId && null != mMessageAdapter) {
                    mMessageAdapter.onReceiverAck(msgLocalID);
                }
            }

            /**
             * 消息发送失败
             *
             * @param msgLocalID 消息本地id
             * @param uid        发送方id
             */
            @Override
            public void onPeerMessageFailure(int msgLocalID, long uid) {
                if (uid == receiverId && null != mMessageAdapter) {
                    mMessageAdapter.onSendFail(msgLocalID, uid);
                }
            }

            /**
             * 用户异地登录,需下线当前用户.
             */
            @Override
            public void onReset() {
                //异地登录,下线用户
                mIMService.stop();
            }
        });
    }

    private void initHeaderView() {
        mHeaderFragment =
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


    private void startPushService() {
        // 注册消息接受者
        SmartPush.registerReceiver(new IMsgReceiver() {
            @Override
            public void onMessage(String message) {
                // message为透传消息，需开发者在此处理
                Log.i("PUSH", "透传消息:" + message);
            }

            @Override
            public void onDeviceToken(byte[] tokenArray) {
                // SmartPushOpenUtils是 sdk提供本地化deviceToken的帮助类，开发者也可以自己实现本地化存储deviceToken
                String deviceTokenStr = null;
                if (null != tokenArray && tokenArray.length > 0) {
                    deviceTokenStr = SmartPushOpenUtils.convertDeviceTokenArrary(tokenArray);
                }
                SmartPushOpenUtils.saveDeviceToken(ChatActivity.this, deviceTokenStr);
                // 玩家已登录
                // ***用于接收推送, 一定要调用该接口后才能接受推送
                AnalysticAgent.bindPlayerIdToToken(ChatActivity.this, deviceTokenStr,
                    String.valueOf(senderId));
            }
        });
        // 注册服务，并启动服务
        SmartPush.registerService(this);
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
                //建立消息对象
                IMMessage msg = new IMMessage();
                //设置发送方id
                msg.sender = senderId;
                //设置接收方id
                msg.receiver = receiverId;
                //消息本地id
                msg.msgLocalID = msgLocalId++;
                //设置消息内容
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
