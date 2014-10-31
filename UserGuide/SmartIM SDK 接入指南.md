# SmartIM SDK 接入指南




## 目录

1. [产品说明](#1)

	1.1 [功能](#1.1)

	1.2 [特点](#1.2)

	1.3 [SDK 包内容](#1.3)

	1.4 [Android SDK版本](#1.4)

2. [接入准备](#2)

	2.1 [IM SDK快速接入说明(必读)](#2.1)
	
	2.2 [成为一个开发者](#2.2)
	
3. [快速集成SDK](#3)
	
	3.1 [依赖建立](#3.1)
	
	3.2 [AndroidManifest.xml配置](#3.2)
	
	3.3 [添加IM代码](#3.3)
	
	3.4 [启动IM服务](#3.4)
				
	3.5 [停止IM服务](#3.5)
	
	3.6 [发送消息](#3.6)
	
4. [相关API以及类说明](#4)
	
	4.1 [IM API](#4.1)
	
	4.2 [相关类介绍](#4.2)




<h2 id="1">1. 产品说明</h2>
<h3 id="1.1">1.1 功能</h3>
* 实现IM功能

<h3 id="1.2">1.2 特点</h3>

* 高效稳定的实现IM功能
* 轻便快捷的api调用,0.5天内能实现IM功能的接入调试
* 简洁明了的IM Demo

<h3 id="1.3">1.3 SDK 包内容</h3>

* SDK开发包：**smart-im-v1.x.x.jar**
* 开发文档：**Smart IM SDK 接入指南.pdf**
* 示例程序工程：**IMDemo**


<h3 id="1.4">1.4 Android SDK版本</h3>

* 目前SDK只支持Android 2.2或以上版本的手机系统。

<h2 id="2">2. 快速集成SDK</h2>


<h3 id="2.1">2.1 IM SDK接入前准备(必读)</h3>
*  IM的SDK是需要和Push的SDK一并使用, 请先移步阅读[Push SDK说明文档](http://docs.gameservice.com/push/Android-SDK.html)
  

<h3 id="2.2">2.2 成为一个开发者</h3>
*  在网站注册成为开发者,并创建一个属于您的应用配置好对应包名,此时可以获取该应用在Android端所匹配的AppId和AppKey,文档中示例AppId为10781,AppKey为cZe1eqiDmQG4T5wHkzOykGdbZvq6oQAo. 接下来,在IMDemo/AndroidManifest.xml文件中中配置将这AppId和AppKey替换为您申请应用对应的AppId和AppKey. 

*  AndroidManifest.xml文件中meta-data 标签配置说明：
	* NGDS_APPID（应用ID）的value值配置成您在<a href="http://developers.gameservice.com/">GameService 开发网站</a>里所添加游戏对应的AppId。
	* NGDS_APPKEY（应用KEY）的value值配置成您在<a href="http://developers.gameservice.com/">GameService 开发网站</a>里所添加游戏对应的AppKey。 

		
		
			<application>
				...
				 <!-- 必需： 应用ID -->
	        	<meta-data
	           	 android:name="NGDS_APPID"
	           	 android:value="10781" />
	
		        <!-- 必需： 应用KEY -->
		        <meta-data
		            android:name="NGDS_APPKEY"
		            android:value="cZe1eqiDmQG4T5wHkzOykGdbZvq6oQAo" />

* 配置好AppId和AppKey之后可分别将demo运行在两部手机中(模拟器也可以),在登录页面中可将其中一台手机设置本机用户id为10001,接收用户id为10002.将另外一台手机设置为本机id为10002,接收用户id为10001.登录之后便可以体验IM功能.

<h2 id="3">3 快速集成SDK</h2>


<h3 id="3.1">3.1 依赖建立</h3>

* 拷贝 smart-push-im-v1.x.x.jar 包到主工程的libs下；

* Eclipse 下导入依赖包

	注意：使用 Eclipse ADT 17 以上版本的开发者，其可自动引用jar包。 使用 Eclipse ADT 17 以下版本开发者使用老方式添加工程引用：右键工程根目录 > Properties > Java Build Path > Libraries，然后点击Add External JARs... 选择指向jar包的路径(主项目的libs目录下),点击OK. 

* IntelliJ IDEA 下导入依赖包

	工程libs目录 > 右键  smart-push-im-v1.x.x.jar > Add as Library > 选择Project Library > 加为项目依赖.
 
<h3 id="3.2">3.2 AndroidManifest.xml配置</h3>


*  以下配置可以在IMDemo/AndroidManifest.xml 找到并直接拷贝放置进自己的应用工程中的AndroidManifest.xml文件对应的tag中.

*  manifest 标签下添加：

	    <!-- 必需： 权限配置 -->
	    <uses-permission android:name="android.permission.INTERNET" />
	    <uses-permission android:name="android.permission.BATTERY_STATS" />
	    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
	    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
	    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
	    <uses-permission android:name="android.permission.VIBRATE" />
	    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	    <uses-permission android:name="android.permission.BROADCAST_STICKY" />
	    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
	    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
	    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
	    <uses-permission android:name="android.permission.WAKE_LOCK" />


*  application 标签下添加：

		<!-- 必需： 应用ID(此处的id为您申请的应用id) -->
        <meta-data
            android:name="NGDS_APPID"
            android:value="8" />
        
        <!-- 必需： 应用KEY (此处appkey为您申请的应用密钥)-->
        <meta-data
            android:name="NGDS_APPKEY"
            android:value="sVDIlIiDUm7tWPYWhi6kfNbrqui3ez44" />

        <!-- 必需： 推送页面配置 -->
        <activity android:name="com.gameservice.sdk.push.ui.SmartPushActivity" />

        <!-- 必需： push 服务配置 -->
        <service
            android:name="com.gameservice.sdk.push.api.SmartPushService"
            android:process=":ngds" />

        <!-- 必需： push 消息接收配置 -->
        <receiver
            android:name="com.gameservice.sdk.push.api.SmartPushReceiver"
            android:enabled="true"
            android:exported="false"
            android:priority="90000"
            android:process=":ngds">
            <intent-filter>
                <action android:name="android.intent.action.PACKAGE_ADDED" />
                <action android:name="android.intent.action.PACKAGE_CHANGED" />
                <action android:name="android.intent.action.PACKAGE_DATA_CLEARED" />
                <action android:name="android.intent.action.PACKAGE_INSTALL" />
                <action android:name="android.intent.action.PACKAGE_REMOVED" />
                <action android:name="android.intent.action.PACKAGE_REPLACED" />
                <action android:name="android.intent.action.PACKAGE_RESTARTED" />
                <action android:name="android.intent.action.USER_PRESENT" />
                <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="cn.ngds.android.intent.alarm" />
            </intent-filter>
        </receiver>
		
		
<h3 id="3.3">3.3 添加IM代码</h3>
* IM服务包括在线聊天以及离线IM推送.([推送功能说明](#2.1))]

* 启动IM服务,代码以及使用场景可在 cn.ngds.im.demo.view.chat.ChatActivity 中找到使用场景,代码及说明如下:

<h3 id="3.4">3.4 开始IM服务</h3>

* 在开始IM服务之前需要设置用户Id以及添加IMServiceObserver监听状态回调.

		//获取IMService
        mIMService = IMService.getInstance();
        //设置使用者Id(为long)
        mIMService.setUID(UserHelper.INSTANCE.getSenderId());
        //注册接受消息状态以及送达回调的观察者
        mIMService.addObserver(new IMServiceObserver() {
            /**
             * 连接状态改变
             * @param state
             */
            @Override
            public void onConnectState(IMService.ConnectState state) {
                if (null == state) {
                    return;
                }
                String status = null;
                switch (state) {
                    case STATE_CONNECTING:
                        status = "连接中...";
                        break;
                    case STATE_CONNECTED:
                        status = "已连接"
                        break;
                    case STATE_CONNECTFAIL:
                        status = "连接失败";
                        break;
                    case STATE_UNCONNECTED:
                        status = "未连接";
                        break;
                }
            }

            /**
             * 收到IM消息
             *
             * @param msg 消息
             */
            @Override
            public void onPeerMessage(IMMessage msg) {
            }

            /**
             * 服务器已收到发送消息回调
             *
             * @param msgLocalID 消息本地id
             * @param uid        发送方id
             */
            @Override
            public void onPeerMessageACK(int msgLocalID, long uid) {
            }

            /**
             * 接收方已收到
             *
             * @param msgLocalID 消息本地id
             * @param uid        发送方id
             */
            @Override
            public void onPeerMessageRemoteACK(int msgLocalID, long uid) {
            }

            /**
             * 消息发送失败
             *
             * @param msgLocalID 消息本地id
             * @param uid        发送方id
             */
            @Override
            public void onPeerMessageFailure(int msgLocalID, long uid) {
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

<h3 id="3.5">3.5 停止IM服务</h3>

* 当程序切换到后台或者退出时需要停止当前IM服务这时候通过Push service进行消息的离线接收.

	 	IMService.getInstance().stop();

* 切换至后台关闭IMService转由PushService接收的实现可以参照IMDemo中cn.ngds.im.demo.view.base.BaseActivy的使用场景(在Activity进入onStop的时候是否app在前台)也可以根据使用需求让开发者自行拓展.

* 监听网络变化,在断开网络的时候主动调用停止服务API停止IM服务,防止socket在后台不断的间歇性重连.网络连接上的时候开启服务.网络变化监听代码在 cn.ngds.im.demo.receiver.NetworkStateReceiver.

		IMService.getInstance().start();
		
<h3 id="3.6">3.6 发送消息</h3>

* 启动IM服务设置好用户id之后便可以开始发送消息,消息发送的代码可以在UI线程以及非UI线程中任意调用 :

		  //建立消息对象
         IMMessage msg = new IMMessage();
         //设置发送方id
         msg.sender = senderId;
         //设置接收方id
         msg.receiver = receiverId;
         //消息本地id
         msg.msgLocalID = msgLocalId++;
         //设置消息内容
         msg.content = "早上好";
         IMService.sendPeerMessage(msg);



<h2 id="4">4 相关API以及类说明</h2>

<h3 id="4.1">4.1 IM API</h3>
*  设置用户Id
	* 调用方法： IMService.getInstance.**setUID**(long userId)
	* 使用场景： 在调用IMService.start方法前调用
	* 参数说明： 
		* userId       玩家id

*  设置观察者
	* 调用方法： IMService.getInstance.**addObserver**(IMServiceObserver imserviceObserver)
	* 使用场景： 观察IMService状态回调
	* 参数说明： 
		* imserviceObserver    IMService 观察者

*  开启服务
	* 调用方法： IMService.getInstance.**start**()
	* 使用场景： 在设置用户id之后调用启动服务,以及在恢复网络或者应用返回前台的时候调用.
	
*  关闭服务
	* 调用方法： IMService.getInstance.**stop**()
	* 使用场景： 退出应用或者应用进入后台的时候调用停止IMService等待PushService接收离线消息.
	
<h3 id="4.2">4.2 相关类介绍</h3>

<h3 id="4.2.1">4.2.1 IMServiceObserver</h3>
* IMService 观察者接口,接口中的所有方法都在UI线程中被调用.

| 方法        		| 功能           	|   
| -----------------	|-------------	    | 
| onConnectState    	| 连接状态改变的回调| 
| onPeerMessage    	| 收到IM消息| 
| onPeerMessageACK    | 服务器收到消息回调| 
| onPeerMessageRemoteACK| 接收方已收到回调| 
| onPeerMessageFailure    | 消息发送失败回调|
| onReset    | 当前用户异地登录|  

* IMMessage 消息体(在app中一般需要用户进一步封装实现UI展示等功能,可以参照IMDemo中的cn.ngds.im.demo.NgdsMessage中的简单封装)


| 属性        		| 说明           	|   
| -----------------	|-------------	    | 
| sender    	| 发送者id| 
| receiver    	| 接收者id| 
| timestamp    | 消息时间戳(只有收到的消息中含有)| 
| msgLocalId| 消息id(接收到的消息没有本地id)| 
| content    | 消息内容|

	


