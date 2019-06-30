/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.beetle.bauhinia.MessageActivity;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Text;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.RoomMessage;
import com.beetle.im.RoomMessageObserver;

import java.util.List;

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
    }



    protected void saveMessage(IMessage imsg) {
        imsg.msgLocalID = msgLocalID++;
    }



    @Override
    protected void sendMessage(IMessage imsg) {
        RoomMessage rm = new RoomMessage();
        rm.sender = imsg.sender;
        rm.receiver = imsg.receiver;
        rm.content = imsg.content.getRaw();
        IMService im = IMService.getInstance();
        im.sendRoomMessageAsync(rm);
    }

    protected void sendTextMessage(String text, List<Long> at, List<String> atNames) {
        IMessage imsg = new IMessage();
        imsg.sender = this.currentUID;
        imsg.receiver = this.roomID;
        MessageContent content = Text.newText(text, at, atNames);
        imsg.setContent(content);
        imsg.timestamp = now();
        imsg.isOutgoing = true;
        saveMessage(imsg);
        loadUserName(imsg);

        sendMessage(imsg);
        imsg.setAck(true);
        insertMessage(imsg);
    }


}
