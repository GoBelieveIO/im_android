package com.example.imservice;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.gameservice.sdk.im.IMMessage;
import com.gameservice.sdk.im.IMPeerMessageHandler;
import com.gameservice.sdk.im.IMService;
import com.gameservice.sdk.im.IMServiceObserver;


public class IMActivity extends Activity implements IMServiceObserver, IMPeerMessageHandler {
    private IMService im;
    private final String TAG = "imservice";

    private EditText input;

    private TextView textView;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.textView = (TextView)findViewById(R.id.text);
        TextView tv = (TextView)findViewById(R.id.textView);
        tv.setText("当前 playerid:2000");
        input = (EditText)findViewById(R.id.editText);
        Log.i(TAG, "start im service");
        im = IMService.getInstance();
        im.setHost("172.25.1.111");
        im.setPort(23000);
        im.setUID(2000);
        im.setPeerMessageHandler(this);
        im.addObserver(this);
        im.start();
    }

    public void onSend(View v) {
        IMMessage msg = new IMMessage();
        msg.sender = 2000;
        msg.receiver = 1000;
        msg.content = input.getText().toString();
        msg.msgLocalID = 1;
        im.sendPeerMessage(msg);
        input.setText("");
    }

    //多点登录
    public void onReset() {
        im.stop();
    }
    public void onConnectState(IMService.ConnectState state) {

    }
    public void onPeerMessage(IMMessage msg) {

        Log.i(TAG, "recv msg:" + msg.content);
        this.textView.setText(msg.content);
    }
    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack");
    }
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {

    }
    public void onPeerMessageFailure(int msgLocalID, long uid) {

    }

    public boolean handleMessage(IMMessage msg) {
        //可在此处保存消息
        msg.msgLocalID = 1;
        return true;
    }
    public boolean handleMessageACK(int msgLocalID, long uid) {
        return true;
    }
    public boolean handleMessageRemoteACK(int msgLocalID, long uid) {
        return true;
    }
    public boolean handleMessageFailure(int msgLocalID, long uid) {
        return true;
    }
}
