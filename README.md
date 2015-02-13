IM SDK
-------------------


##IOS client 接入流程

1.配置
    [IMService instance].host = $HOST
    [IMService instance].port = $PORT

2.设置当前登录的玩家id
    [IMService instance].uid = $UID

3.设置MessageHandler(可选)
    [IMService instance].peerMessageHandler = $Handler

4.监听消息
    [[IMService instance] addMessageObserver:$Observer]

5.取消监听
    [[IMService instance] removeMessageObserver:$Observer]
    
6.启动im
    [[IMService instance] start]

7.停止im
    [[IMService instance] stop]


###android client 接入流程

1.配置
    IMService.getinstance.setHost($HOST)
    IMService.getInstance.setPort($PORT)

2.设置当前登录的玩家id
    IMService.getInstance.setAccessToken($Token)

3.设置MessageHandler(可选)
    IMService.getInstance.setPeerMessageHandler($Handler)

4.监听消息
    IMService.getInstance.addMessageObserver($Observer)
    
5.取消监听
    IMService.getInstance.removeMessageObserver($Observer)
    
6.启动im
    IMService.getInstance.start()

7.停止im
    IMService.getInstance.stop()
    
