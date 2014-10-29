# IM SDK 接入指南

<a href="../../static/download/SmartPush_Android_SDK_V1.1.2.zip" target="_blank" class="sdk-download">下载Android IM SDK</a>


## 目录

1. [产品说明](#1)

	1.1 [功能](#1.1)

	1.2 [特点](#1.2)

	1.3 [SDK 包内容](#1.3)

	1.4 [Android SDK版本](#1.4)

2. [快速集成SDK](#2)

	2.1 [依赖建立](#2.1)

	2.2 [](#2.2)

	2.3 [添加推送代码](#2.3)

	2.4 [测试确认](#2.4)


<h2 id="1">1. 产品说明</h2>
<h3 id="1.1">1.1 功能</h3>
* 实现IM功能

<h3 id="1.2">1.2 特点</h3>

* 高效稳定的实现IM功能
* 0.5天内能实现IM功能的接入调试
* 简洁明了的IM Demo

<h3 id="1.3">1.3 SDK 包内容</h3>

* SDK开发包：**smart-push-v1.x.x.jar**
* 开发文档：**SmartPush SDK 接入指南.pdf**
* 示例程序工程：**SmartPushExample**


<h3 id="1.4">1.4 Android SDK版本</h3>

* 目前SDK只支持Android 2.2或以上版本的手机系统。

<h2 id="2">2. 快速集成SDK</h2>
<h3 id="2.1">2.1 依赖建立</h3>

<h3 id="2.2">2.2 AndroidManifest.xml配置</h3>
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
		
		
*  meta-data 标签配置说明：
	* NGDS_APPID（应用ID）的value值配置成您在<a href="http://developers.gameservice.com/">GameService 开发网站</a>里所添加游戏对应的AppId。
	* NGDS_APPKEY（应用KEY）的value值配置成您在<a href="http://developers.gameservice.com/">GameService 开发网站</a>里所添加游戏对应的AppKey。


