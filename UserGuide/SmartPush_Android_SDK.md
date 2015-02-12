# SmartPush SDK 接入指南

<a href="../../static/download/SmartPush_Android_SDK.zip" target="_blank" class="sdk-download">下载Android  SDK</a>

1、产品说明
===========
##1.1 功能

* 开发者能够主动、及时地向玩家发送通知或消息。
* 接收通知与自定义消息，向 App 传递相关信息。 

##1.2 特点

* 通过用户数据分析挖掘，为开发者定义用户群体。根据实际运营目的，做真正意义上的精确化推送；
* 通过图表清晰对比每一次的推送的效果，每一个节点游戏数据的变化尽在掌控之中。
* 推送（数据指标）、游戏内IM、玩家P2P社交等，紧紧围绕游戏而设计，增强游戏黏性，为游戏而生。
* SDK快速接入，仅需0.5-1天的开发周期。不断完善的文档中心随手查阅，还有客服跟踪与技术对接。
* 新游智能推送将互联网免费精神贯彻到底，所有开放功能均免费使用，实实在在为开发者节约成本。

##1.3 SDK 包内容

* SDK开发包：**gameservice-push-v1.x.x.jar**
* 开发文档：**SmartPush SDK 接入指南.pdf**
* 示例程序工程：**SmartPushExample**

	* SmartPushExample/src下: 示例代码

* 示例程序安装包：**SmartPushExample.apk**

##1.4 Android SDK版本

* 目前SDK只支持Android 2.2或以上版本的手机系统。

##1.5 其他平台支持
* coco2dx平台Android支持包：<a href="../../static/download/cocos2dx-support-v1.1.3.zip" > cocos2dx-support-v1.1.3</a>
* unity3d平台Android平台支持包：<a href="../../static/download/unity3d-support-v1.1.3.zip" > unity3d-support-v1.1.3</a>


2、快速集成SDK
===================
##2.1 依赖建立

* 拷贝 gameservice-push-v1.x.x.jar 以及gameservice-analystic-v1.x.x.jar包到主工程的libs下；

* Eclipse 下导入依赖包

	注意：使用 Eclipse ADT 17 以上版本的开发者，其可自动引用jar包。 使用 Eclipse ADT 17 以下版本开发者使用老方式添加工程引用：右键工程根目录 > Properties > Java Build Path > Libraries，然后点击Add External JARs... 选择指向jar包的路径(主项目的libs目录下)，点击OK，流程如下：
		
	![](../Android-SDK-Pic/SetEclipse_1.jpg)
	
	![](../Android-SDK-Pic/SetEclipse_2.jpg)

	![](../Android-SDK-Pic/SetEclipse_3.jpg)


* IntelliJ IDEA 下导入依赖包

	工程libs目录 > 右键  smart-push-v1.x.x.jar > Add as Library > 选择Project Library > 加为项目依赖：

	![](../Android-SDK-Pic/SetIdea_1.jpg)

	![](../Android-SDK-Pic/SetIdea_2.jpg)


##2.2 AndroidManifest.xml配置

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

		<!-- 必需： 应用ID -->
        <meta-data
            android:name="NGDS_APPID"
            android:value="8" />
        
        <!-- 必需： 应用KEY -->
        <meta-data
            android:name="NGDS_APPKEY"
            android:value="sVDIlIiDUm7tWPYWhi6kfNbrqui3ez44" />

        <!-- 必需： 推送页面配置 -->
        <activity android:name="SmartPushActivity" />

        <!-- 必需： push 服务配置 -->
        <service
            android:name="SmartPushService"
            android:process=":ngds" />

        <!-- 必需： push 消息接收配置 -->
        <receiver
            android:name="SmartPushReceiver"
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
		
		
*  meta-data 标签配置说明：
	* NGDS_APPID（应用ID）的value值配置成您在<a href="http://developers.gameservice.com/">GameService 开发网站</a>里所添加游戏对应的AppId。
	* NGDS_APPKEY（应用KEY）的value值配置成您在<a href="http://developers.gameservice.com/">GameService 开发网站</a>里所添加游戏对应的AppKey。

##2.3 添加推送代码

* 推送服务包括：
	* 推送通知：通知以状态栏Notification的形式展现，无需开发者处理。
	* 推送消息：处理消息需开发者实现 IMsgReceiver 子类。


* 简易的接入流程可以参照Demo中BaseApp内的startPushService()方法,如下：(IMsgReceiver类，请参考[IMsgReceiver类介绍](#5.1))

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
                    deviceTokenStr = DeviceTokenUtil.bin2Hex(tokenArray);
                }
                SmartPushOpenUtils.saveDeviceToken(BaseApp.this, deviceTokenStr);
                if (!TextUtils.isEmpty(sPlayerId)) {
                    // 玩家已登录
                    // ***用于接收推送, 一定要调用该接口后才能接受推送，请绑定玩家的playerId
                    AnalysticAgent
                        .bindPlayerIdToToken(BaseApp.this, deviceTokenStr, sPlayerId);
                } else {
                    // ***用于接收推送,一定要调用该接口后才能接受推送,若游戏没有playerId，请绑定唯一的设备Id
                    AnalysticAgent.bindPlayerIdToToken(BaseApp.this, deviceTokenStr,
                        DemoHelper.getDeviceId(sAppContext));
                }
            }
        });
        // 注册服务，并启动服务
        SmartPush.registerService(this);

*  **注意事项:**当前推送服务采取区分进程的启动方式,会导致项目中Application的onCreate方法被调用两次(目前采用android:process=":string"的service配置的方式都会引起该结果).望开发者酌情处理.
				
##2.4 测试确认

* gameservice-push-v1.x.x.jar 以及gameservice-analystic-v1.x.x.jar包是否添加为依赖。
* 请确认 AndroidManifest.xml 中的**NGDS_APPKEY**和**NGDS_APPID**的value值是否正常填写。
* 请确认主项目包名是否与<a href="http://developers.gameservice.com/">GameService 开发网站</a>里相应的App一致。

###2.4.1 测试推送通知

* 登入<a href="http://developers.gameservice.com/push">GameService 开发网站</a>，在要推送应用下点击创建推送 > 网页推送 > 推送通知：

	![](../Android-SDK-Pic/TestSDK_1.jpg)

	![](../Android-SDK-Pic/TestSDK_2.jpg)



###2.4.2 测试推送消息

* 登入<a href="http://developers.gameservice.com/push">GameService 开发网站</a>，在要推送应用下点击创建推送 > 网页推送 > 推送消息：

	![](../Android-SDK-Pic/TestSDK_1.jpg)

	![](../Android-SDK-Pic/TestSDK_7.jpg)

3、采集的配置与使用
===============

* SDK提供采集功能，开发者调用采集API可记录用户行为，并在<a href="http://developers.gameservice.com/">GameService 开发网站</a>会呈现“日活跃用户数量”等统计信息；商家可依据这些采集到的行为进行分析作出针对性推送，增加高活跃用户，提高盈利。**若不使用采集API，无法使用此功能**。

##3.1 添加采集代码
* 在Activity的onResume和onPause方法中作如下调用，以此来支持采集API的使用,若不添加如下代码，采集登入登出的相关API无法正常使用

		@Override
	    protected void onPause() {
	        AnalysticAgent.onActivityPause(this);
	        super.onPause();
	    }
	
	    @Override
	    protected void onResume() {
	        AnalysticAgent.onActivityResume(this);
	        super.onResume();
	    }

* 玩家连接服务器后添加以下代码，记录当前服务器id和对应名称：

        // 这边serverId和serverName规则为开发者自定义，键值对形式
        AnalysticAgent.recordServer("1001", "春暖花开");

* 应用开始时即可记录当前渠道id和渠道id对应名称：

        // 这边channelId和channelName规则为开发者自定义，键值对形式
        AnalysticAgent.recordChannel("1", "新游互联");

* SDK提供了采集玩家各种行为的API，供开发者使用，调用方式和场景请参考[采集API介绍](#4.2)。

##3.2 查看统计信息

* 登入<a href="http://developers.gameservice.com/">GameService 开发网站</a>，在要查看的应用下点击：推送记录 > 精确推送 > 提高在线玩家数，即可查看统计信息:

	![](../Android-SDK-Pic/Collection_1.jpg)	

##3.3 自定义规则

* 登入<a href="http://developers.gameservice.com/">GameService 开发网站</a>，在要查看的应用下点击：推送记录 > 精确推送 > 提高在线玩家数 > 自定义规则 ，根据采集信息创建特定推送规则。

	![](../Android-SDK-Pic/Collection_2.jpg)	

	![](../Android-SDK-Pic/Collection_3.jpg)	

4、API 介绍
====================

##4.1 推送 API

* 注册启动push服务
	* 调用方法： SmartPush.**registerService** (Context context) 
	* 使用场景： 开发者需要开启push服务时调用
	* 参数说明： context 当前上下文

* 注册消息接收者
	* 调用方法： SmartPush.**registerReceiver**(IMsgReceiver receiver)
	* 使用场景： 开发者需要开启push服务前，注册IMsgReceiver
	* 参数说明： receiver消息处理接口类

* 注销消息接收者
	* 调用方法： SmartPush.**unRegisterReceiver**()
	* 使用场景： 可在页面消亡时解除绑定
	* 参数说明： 无

* 是否显示设置通知提示(适用于小米/魅族以及其他部分会配置关闭通知以及自启的手机)
	* 调用方法： SmartPush.**showGuideOnce**(Activity activity)
	* 使用场景： 可在app启动之后择时机调用(只会显示一次)
	* 参数说明： activity 当前activity

* 是否匹配指定会导致推送可能无法正确显示的机型(适用于小米/魅族以及其他部分会配置关闭通知以及自启的手机)
	* 调用方法： SmartPush.**matchTargeModel**(Context context)
	* 使用场景： 判断是否是需要指引用户设置显示通知的机型

* 弹出显示设置通知提示(适用于小米/魅族以及其他部分会配置关闭通知以及自启的手机)
	* 调用方法： SmartPush.**showGuide**(Activity activity)
	* 参数说明： activity

##4.2 采集 API
* 玩家绑定设备接口(**此方法用于正常收到推送,必须调用**)
	* 调用方法： AnalysticAgent.**bindPlayerIdToToken**(Context context, String deviceToken, String playerId)
	* 使用场景： 当成功取得deviceToken时，需调用此方法使玩家id和设备绑定
	* 参数说明： 
		* context       当前上下文
		* deviceToken   设备token
		* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")

* 记录玩家离开当前页面行为
	* 调用方法： AnalysticAgent.**onActivityPause**(Context context)
	* 使用场景： 当Activity进入onPause状态时调用
	* 参数说明： 
		* context       当前上下文

* 记录玩家返回当前页面行为
	* 调用方法： AnalysticAgent.**onActivityResume**(Context context)
	* 使用场景： 当Activity进入onResume状态时调用
	* 参数说明： context       当前上下文

* 记录玩家登入
	* 调用方法： AnalysticAgent.**recordLogin**(Context context, String playerId)
	* 使用场景： 当玩家登入时调用
	* 参数说明：      
		* context       当前上下文
		* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")

* 记录玩家登出
	* 调用方法： AnalysticAgent.**recordLogout**(Context context)
	* 使用场景： 当玩家登出时调用
	* 参数说明： context    当前上下文

* 记录当前服务器id和对应名称
	* 调用方法： AnalysticAgent.**recordServer**(String serverId, String serverName)
	* 使用场景： 当玩家连接上服务器时调用
	* 参数说明： 
		* serverId   服务器id,开发者服务器自定义id
		* serverName 服务器id对应名称

* 记录当前渠道id和渠道id对应名称
	* 调用方法： AnalysticAgent.**recordChannel**(String channelId, String channelName)
	* 使用场景： 可在应用开始时调用
	* 参数说明： 
    	* channelId   渠道id,开发者自定义渠道id
    	* channelName 渠道id对应名称   	

* 记录玩家充值
	* 调用方法： AnalysticAgent.**recordPay**(Context context, String playerId,
		float amount, String paymentChanel,
		String currency, int coinAmount, 
		String orderId, int level)
	* 使用场景： 当玩家完成充值后调用
	* 参数说明：     
		* context       当前上下文
		* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
		* amount        充值金额
		* paymentChanel 支付渠道
		* currency      金额币种
		* coinAmount    充值游戏币个数
		* orderId       订单号
		* level         玩家等级(0-100的数字)	

* 记录玩家开始游戏的时间
	* 调用方法： AnalysticAgent.**recordPlayerLevelStartTime**(Context context,  String playerId, int playerLevel)
	* 使用场景： 当玩家升级到一个新的等级的时候调用
	* 参数说明： 
    	* context       当前上下文
    	* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
    	* playerLevel   玩家等级(0-100的数字)

* 记录玩家离开游戏的时间
	* 调用方法： AnalysticAgent.**recordPlayerLevelEndTime**(Context context, String playerId, int playerLevel)
	* 使用场景： 当玩家离开这个等级时调用
	* 参数说明： 
    	* context       当前上下文
    	* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
    	* playerLevel   玩家等级(0-100的数字)

* 关于recordPlayerLevelEndTime 以及recordPlayerLevelStartTime的调用说明
	
	* 玩家等级发生变化时
           
           调用recordPlayerLevelEndTime，上报离开的等级
           调用recordPlayerLevelStartTime, 上报进入的等级
       
    * 玩家进入或退出游戏时
           
           进入游戏调用recordPlayerLevelEndTime，上报当前的等级
           退出游戏调用recordPlayerLevelStartTime, 上报当前的等级（跟进入时相比，等级可能已经发生变化，如果等级发生变化，一定要按2.1所说的进行上报）
	
	* 释疑：
    为什么在玩家进入或退出游戏是也要上报等级
    假设某个玩家在1号进入等级5，一个星期后，玩家升级进入等级6。
    
    A、如果不上报玩家每次进入、退出游戏的时间的话，我们只能非常不准确的得出玩家在5这个等级停留了7天的结论
    显然，这个数据不准确，没有任何价值。
    
    B、上报的话，我们可以准确知道用户实际玩的时间，如下场景：
          用户1号进入5级，玩了1分钟，退出游戏，2~6号没玩，7号玩了1分钟，立即从5级升级到6级
          玩家时间在等级5停留了2分钟

* 记录玩家进入关卡的时间
	* 调用方法： AnalysticAgent.**recordMissionStartTime**(Context context, String playerId, String missionId, String missionName)
	* 使用场景： 当玩家进入特定关卡时调用
	* 参数说明： 
    	* context       当前上下文
    	* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
    	* missionId     关卡id(使用100001~199999之间字符串表示)
    	* missionName   关卡id对应名称

* 记录玩家离开关卡的时间
	* 调用方法： AnalysticAgent.**recordMissionEndTime**(Context context, String playerId, String missionId, String missionName)
	* 使用场景： 当玩家离开特定关卡时调用
	* 参数说明： 
    	* context       当前上下文
    	* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
    	* missionId     关卡id(使用100001~199999之间字符串表示)
    	* missionName   关卡id对应名称

* 记录玩家虚拟消费
	* 调用方法： AnalysticAgent.**recordConsumption**(Context context, String playerId, String itemId,
		String itemName,
        int itemAmount,
        int coinAmount)
	* 使用场景： 当玩家在App中有虚拟消费行为时调用
	* 参数说明： 
	    * context       当前上下文
    	* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
    	* itemId        消费商品id
    	* itemName      消费商品id对应名称
    	* itemAmount    消费商品数量
    	* coinAmount    消费虚拟币数量  

* 记录玩家剩余金币
	* 调用方法： AnalysticAgent.**recordBalance**(Context context, String playerId, int balance)
	* 使用场景： 当玩家在App中虚拟消费后调用
	* 参数说明： 
	    * context       当前上下文
    	* playerId      玩家id(为字符串，若没有玩家id，请设置为"0")
    	* coinAmount    消费虚拟币数量  	




5、相关类介绍
====================
##5.1 IMsgReceiver（消息接收接口）

| 分类        		| 功能           	| 方法  	            |
| -----------------	|-------------	    | --------------------  |
| 透传消息接口    	    | 处理透传消息		| onMessage    |
| deviceToken信息接口   | 绑定deviceToken     	|   onDeviceToken   |

* 接收透传消息
	* 方法： public void **onMessage**(String message)
	* 功能： 开发者可以接收到透传消息，并根据业务需求自行处理
	* 参数： message 为收到的透传消息
	
* 接收deviceToken信息
	* 方法： public void **onDeviceToken**(byte[] tokenArray) 
	* 功能： 开发者可以接收到deviceToken信息，用于deviceToken的本地化和玩家id的绑定
	* 参数： tokenArray 为收到的设备token数组,可通过提供的DeviceTokenUtil.bin2Hex(tokenArrary)换成字符串格式.

##5.2 SmartPushOpenUtils 工具类(可选用)

#####一个可以帮助用户实现本地化deviceToken的工具类,具体使用可见Demo
* 本地化DeviceToken
	* 方法： public static void **saveDeviceToken**(Context context, String deviceToken)
	* 功能： 保存DeviceToken
	* 参数： 
		* context      当前上下文 
        * deviceToken  设备token
	
* 取得已经保存的deviceToken
	* 方法： public static String **loadDeviceToken**(Context context)
	* 功能： 取得保存的deviceToken
	* 参数： context      当前上下文 

6、常见问题
=============

##6.1 推送成功了，为什么有部分客户端收不到推送？

* 请再次检查集成步骤是否按照“接入指南”进行;
* 请在logcat查看日志，确定客户端SmartPush是否 “socket connected” ，网络是否有问题;
* 手机的应用设置中是否勾选了“显示通知”;

##6.2 MIUI 系统或小米手机收不到推送通知

* 自启动管理：在自启动管理界面，相应客户端允许自启动，并勾选信任该程序;
* 网络助手：设置相应客户端允许访问2G/3G和WIFI的网络;
* 应用设置：相应客户端的应用设置中勾选了“显示通知”，“通知栏显示”等选项;


##6.3 如何在代码混淆时忽略 gameservice-push-v1.x.x.jar以及gameservice-analystic-v1.x.x.jar？

* 请下载最新的proguard.jar， 并替换你Android Sdk目录下tools\proguard\lib\proguard.jar;
* 在你的proguard.cfg加以下代码：

		-dontwarn com.gameservice.sdk.analystic.**
        -keep class com.gameservice.sdk.analystic.** { *; }
        -dontwarn com.gameservice.sdk.push.**
        -keep class com.gameservice.sdk.push.** { *; }

* 如果是使用新版本的ADT，将project.properties的中“# proguard.config=${sdk.dir}/tools/proguard/proguard-android.txt:proguard-project.txt”的“#”注释去掉，然后在proguard-android.txt中配置;
