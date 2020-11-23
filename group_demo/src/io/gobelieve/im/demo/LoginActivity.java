/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.EPeerMessageDB;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.handler.GroupMessageHandler;
import com.beetle.bauhinia.handler.PeerMessageHandler;
import com.beetle.bauhinia.handler.SyncKeyHandler;
import com.beetle.im.IMService;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.gobelieve.im.demo.model.MessageDatabaseHelper;


/**
 * LoginActivity
 * Description: 登录页面,给用户指定消息发送方Id
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private final String TAG = "demo";

    private EditText mEtAccount;
    private EditText mEtTargetAccount;

    AsyncTask mLoginTask;
    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Button btnLogin = (Button) findViewById(R.id.btn_login);
        mEtAccount = (EditText) findViewById(R.id.et_username);
        mEtTargetAccount = (EditText) findViewById(R.id.et_target_username);
        btnLogin.setOnClickListener(this);
    }

    void openDB(long currentUID) {
        File p = this.getDir("db", MODE_PRIVATE);
        File f = new File(p, String.format("gobelieve_%d.db", currentUID));
        String path = f.getPath();
        MessageDatabaseHelper dh = MessageDatabaseHelper.getInstance();
        dh.open(this.getApplicationContext(), path);
        SQLiteDatabase db = dh.getDatabase();
        Log.i(TAG, "db version:" + db.getVersion());
        PeerMessageDB.getInstance().setDb(db);
        EPeerMessageDB.getInstance().setDb(db);
        GroupMessageDB.getInstance().setDb(db);
        CustomerMessageDB.getInstance().setDb(db);
    }

    private void go2Chat(long sender, long receiver, String token) {
        IMService.getInstance().stop();
        PeerMessageDB.getInstance().setDb(null);
        EPeerMessageDB.getInstance().setDb(null);
        GroupMessageDB.getInstance().setDb(null);
        CustomerMessageDB.getInstance().setDb(null);


        openDB(sender);

        PeerMessageHandler.getInstance().setUID(sender);
        GroupMessageHandler.getInstance().setUID(sender);
        IMHttpAPI.setToken(token);
        IMService.getInstance().setToken(token);

        SyncKeyHandler handler = new SyncKeyHandler(this.getApplicationContext(), "sync_key");
        handler.load();

        HashMap<Long, Long> groupSyncKeys = handler.getSuperGroupSyncKeys();
        IMService.getInstance().clearSuperGroupSyncKeys();
        for (Map.Entry<Long, Long> e : groupSyncKeys.entrySet()) {
            IMService.getInstance().addSuperGroupSyncKey(e.getKey(), e.getValue());
            Log.i(TAG, "group id:" + e.getKey() + "sync key:" + e.getValue());
        }
        IMService.getInstance().setSyncKey(handler.getSyncKey());
        Log.i(TAG, "sync key:" + handler.getSyncKey());
        IMService.getInstance().setSyncKeyHandler(handler);


        IMService.getInstance().start();


        Intent intent = new Intent(this, AppGroupMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("group_id", receiver);
        intent.putExtra("group_name", "测试群");
        intent.putExtra("current_uid", sender);
        startActivity(intent);
        finish();
    }



    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_login) {
            if (mEtAccount.getText().toString().length() <= 0) {
                Toast.makeText(this, "请设置您的用户id", Toast.LENGTH_SHORT).show();
                return;
            }
            final long senderId = Long.parseLong(mEtAccount.getText().toString());
            if (senderId <= 0) {
                Toast.makeText(this, "用户id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mEtTargetAccount.getText().toString().length() <= 0) {
                Toast.makeText(this, "请设置群组id", Toast.LENGTH_SHORT).show();
                return;
            }
            final long receiverId = Long.parseLong(mEtTargetAccount.getText().toString());
            if (receiverId <= 0) {
                Toast.makeText(this, "群组id不能为0或者-1", Toast.LENGTH_SHORT).show();
                return;
            }


            if (mLoginTask != null) {
                return;
            }

            mLoginTask = new AsyncTask<Void, Integer, String>() {
                @Override
                protected String doInBackground(Void... urls) {
                    return LoginActivity.this.login(senderId);
                }
                @Override
                protected void onPostExecute(String result) {
                    mLoginTask = null;
                    if (result != null && result.length() > 0) {
                        //设置用户id,进入MainActivity
                        go2Chat(senderId, receiverId, result);
                    } else {
                        Toast.makeText(LoginActivity.this, "登陆失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();

        }
    }

    private String login(long uid) {
        //调用app自身的登陆接口获取im服务必须的access token,之后可将token保存在本地供下次直接登录IM服务
        String api_url = "http://demo.gobelieve.io";

        String uri = String.format("%s/auth/token", api_url);
        try {
            URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            JSONObject json = new JSONObject();
            json.put("uid", uid);
            int PLATFORM_ANDROID = 2;
            String androidID = Settings.Secure.getString(this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            json.put("platform_id", PLATFORM_ANDROID);
            json.put("device_id", androidID);
            String data = json.toString();

            try(OutputStream os = connection.getOutputStream()){
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != 200){
                System.out.println("login failure code is:" + responseCode);
                return null;
            }
            int len = connection.getHeaderFieldInt("Content-Length", 64*1024);
            byte[] buf = new byte[len];
            InputStream inStream = connection.getInputStream();

            int pos = 0;
            while (pos < len) {
                int n = inStream.read(buf, pos, len - pos);
                if (n == -1) {
                    break;
                }
                pos += n;
            }
            inStream.close();
            if (pos != len) {
                return null;
            }
            String txt = new String(buf, "UTF-8");
            JSONObject jsonObject = new JSONObject(txt);
            String accessToken = jsonObject.getString("token");
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
