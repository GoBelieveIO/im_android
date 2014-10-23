package cn.ngds.im.demo.view.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.domain.NgdsMessage;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.view.base.BaseActivity;
import cn.ngds.im.demo.view.header.HeaderFragment;
import com.gameservice.sdk.im.IMMessage;
import com.gameservice.sdk.im.IMService;
import com.gameservice.sdk.im.IMServiceObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity
 * Description: 用户聊天窗口
 */
public class ChatActivity extends BaseActivity implements IMServiceObserver, View.OnClickListener {
    private ListView mLvChatMsg;
    private MessageAdapter mMessageAdapter;
    private EditText mEtSendBoard;
    private Button mBtnSend;
    private long receiverId;
    private long senderId;
    public static final String KEY_USER_ID = "key_user_id";
    private static int msgLocalId = 1;
    private IMService mIMService;
    private List<NgdsMessage> mChatMsgList;

    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_chat);
        if (getIntent() != null) {
            receiverId = getIntent().getExtras().getLong(KEY_USER_ID);
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
        mIMService = IMService.getInstance();
        mIMService.addObserver(this);
        mChatMsgList = new ArrayList<NgdsMessage>();
        senderId = UserHelper.INSTANCE.getUserId();

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
                finish();
            }
        });

        mHeaderFragment.setRightButtonClickListener(new HeaderFragment.HeaderButtonClickListener() {
            @Override
            public void onHeaderButtonClicked() {


            }
        });
    }

    @Override
    protected void bindView(Bundle savedInstanceState) {

    }


    @Override
    public void onConnectState(IMService.ConnectState state) {

    }

    @Override
    public void onPeerMessage(IMMessage msg) {
        if (null != mMessageAdapter && msg.receiver == senderId) {
            mChatMsgList.add(new NgdsMessage(msg, NgdsMessage.Direct.RECEIVE));
            mMessageAdapter.notifyDataSetChanged();
        }
    }

    private boolean first = true;

    @Override
    public void onPeerMessageACK(int msgLocalID, long uid) {
        if (uid == receiverId && null != mMessageAdapter) {
            mMessageAdapter.onServerAck(msgLocalID, uid);
        }
    }

    @Override
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {

    }

    @Override
    public void onPeerMessageFailure(int msgLocalID, long uid) {
        if (uid == receiverId && null != mMessageAdapter) {
            mMessageAdapter.onSendFail(msgLocalID, uid);
        }
    }

    @Override
    public void onReset() {

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
}
