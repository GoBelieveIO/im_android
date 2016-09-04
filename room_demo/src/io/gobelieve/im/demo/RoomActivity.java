package io.gobelieve.im.demo;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;

import com.beetle.bauhinia.MessageActivity;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.beetle.bauhinia.tools.PeerOutbox;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.RoomMessage;
import com.beetle.im.RoomMessageObserver;

public class RoomActivity extends MessageActivity implements RoomMessageObserver, IMServiceObserver {

    private long currentUID;
    private long roomID;
    private int msgLocalID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        currentUID = intent.getLongExtra("current_uid", 0);
        if (currentUID == 0) {
            Log.e(TAG, "current uid is 0");
            return;
        }


        roomID = intent.getLongExtra("room_id", 0);
        if (roomID == 0) {
            return;
        }

        String name = intent.getStringExtra("room_name");
        if (name == null) {
            Log.e(TAG, "peer name is null");
            return;
        }

        getSupportActionBar().setTitle(name);
        IMService.getInstance().addRoomObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().enterRoom(roomID);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        IMService.getInstance().leaveRoom(roomID);
        IMService.getInstance().removeRoomObserver(this);
        IMService.getInstance().removeObserver(this);
    }

    @Override
    public void onRoomMessage(RoomMessage msg) {
        if (msg.receiver != roomID) {
            return;
        }
        Log.i(TAG, "recv msg:" + msg.content);
        final IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msgLocalID++;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        insertMessage(imsg);
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTED) {
            enableSend();
        } else {
            disableSend();
        }
        setSubtitle();
    }



    void saveMessage(IMessage imsg) {
        imsg.msgLocalID = msgLocalID++;
    }

    boolean sendRoomMessage(IMessage imsg) {
        RoomMessage rm = new RoomMessage();
        rm.sender = imsg.sender;
        rm.receiver = imsg.receiver;
        rm.content = imsg.content.getRaw();
        IMService im = IMService.getInstance();
        return im.sendRoomMessage(rm);
    }

    protected void sendTextMessage(String text) {
        if (text.length() == 0) {
            return;
        }

        IMessage imsg = new IMessage();
        imsg.sender = this.currentUID;
        imsg.receiver = this.roomID;
        imsg.setContent(IMessage.newText(text));
        imsg.timestamp = now();
        imsg.isOutgoing = true;
        saveMessage(imsg);
        boolean sended = sendRoomMessage(imsg);
        if (sended) {
            imsg.setAck(true);
        } else {
            imsg.setFailure(true);
        }
        insertMessage(imsg);
    }
}
