GoBelieve Android SDK
-------------------

##demo各模块说明
app, group_demo, customer_service_demo是application模块
asynctcp, imsdk, imkit是library模块

1. app模块测试点对点消息
    app模块可以输入自己和对方的uid(整型),就可以直接和对方聊天.
    app模块如果只输入自己的uid，那么会进入会话列表界面
3. group_demo模块测试群组消息
    group_demo模块可以输入自己的uid和群组的id(整型),就可以直接测试群组消息
5. customer_service_demo测试客服消息
    customer_service_demo模块是以客服人员的身份登录,并接受用户发来的客服消息

##应用集成到客户端
1. import asynctcp, imsdk, imkit模块到自己的app工程
2. 在Application的onCreate初始化

        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        //设置设备唯一标识,用于多点登录时设备校验
        IMService.getInstance().setDeviceID(androidID);

        //监听网路状态变更
        IMService.getInstance().registerConnectivityChangeReceiver(getApplicationContext());

        mIMService.setPeerMessageHandler(PeerMessageHandler.getInstance());
        mIMService.setGroupMessageHandler(GroupMessageHandler.getInstance());
        mIMService.setCustomerMessageHandler(CustomerMessageHandler.getInstance());

3. 登录成功后设置uid,token

        IMService.getInstance().setToken(token);
        PeerMessageHandler.getInstance().setUID(uid);
        GroupMessageHandler.getInstance().setUID(uid);

        SyncKeyHandler handler = new SyncKeyHandler(this.getApplicationContext(), String.format("sync_key_%d", uid));
        handler.load();
        IMService.getInstance().setSyncKeyHandler(handler);

4. 打开消息db, 数据库表结构参照demo中的MessageDatabaseHelper源代码

        File p = this.getDir("db", MODE_PRIVATE);
        File f = new File(p, String.format("gobelieve_%d.db", uid));
        String path = f.getPath();
        MessageDatabaseHelper dh = MessageDatabaseHelper.getInstance();
        dh.open(this.getApplicationContext(), path);
        SQLiteDatabase db = dh.getDatabase();
        PeerMessageDB.getInstance().setDb(db);
        EPeerMessageDB.getInstance().setDb(db);
        GroupMessageDB.getInstance().setDb(db);
        CustomerMessageDB.getInstance().setDb(db);

5. 启动IMService接受消息

        IMService.getInstance().start();

6. 添加消息observer，处理相应类型的消息

        //连接状态
        IMService.getInstance().addObserver(ob);

        //点对点消息
        IMService.getInstance().addPeerObserver(ob);
        //群组消息
        IMService.getInstance().addGroupObserver(ob);
        //直播的聊天室消息
        IMService.getInstance().addRoomObserver(ob);
        //实时消息,用于voip的信令
        IMService.getInstance().addRTObserver(ob);
        //系统消息
        IMService.getInstance().addSystemObserver(ob);    

7. 应用进入后台，断开socket链接

        IMService.getInstance().enterBackground();

8. 应用返回前台,重现链接socket
 
        IMService.getInstance().enterForeground();

9.  发送点对点消息

        Intent intent = new Intent(this, PeerMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", peer_uid);
        intent.putExtra("peer_name", "");
        intent.putExtra("current_uid", uid);
        startActivity(intent);

10. 发送群组消息

        Intent intent = new Intent(this, GroupMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("group_id", groupId);
        intent.putExtra("group_name", "");
        intent.putExtra("current_uid", uid);
        startActivity(intent);

11. 用户注销

        IMService.getInstance().stop()