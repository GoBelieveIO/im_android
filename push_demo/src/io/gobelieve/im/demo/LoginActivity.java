package io.gobelieve.im.demo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.beetle.bauhinia.PeerMessageActivity;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.im.IMService;

import java.nio.charset.StandardCharsets;


import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * LoginActivity
 * Description: 登录页面,给用户指定消息发送方Id
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
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

    private void go2Chat(long sender, long receiver, String token) {
        IMService.getInstance().setToken(token);

        IMHttpAPI.setToken(token);
        IMService.getInstance().setToken(token);
        IMService.getInstance().setUID(sender);

        PushDemoApplication.getApplication().bindDeviceTokenToIM();
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
                Toast.makeText(this, "请设置接收者的用户id", Toast.LENGTH_SHORT).show();
                return;
            }
            final long receiverId = Long.parseLong(mEtTargetAccount.getText().toString());
            if (receiverId <= 0) {
                Toast.makeText(this, "接收方id不能为0或者-1", Toast.LENGTH_SHORT).show();
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
        //调用app自身的登陆接口获取im服务必须的access token
        String URL = "http://demo.gobelieve.io";
        String uri = String.format("%s/auth/token", URL);

        try {
            java.net.URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-type", "application/json");
            connection.connect();

            JSONObject json = new JSONObject();
            json.put("uid", uid);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
            writer.write(json.toString());
            writer.close();

            int responseCode = connection.getResponseCode();
            if(responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("login failure code is:" + responseCode);
                return null;
            }

            InputStream inputStream = connection.getInputStream();

            //inputstream -> string
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String str = result.toString(StandardCharsets.UTF_8.name());


            JSONObject jsonObject = new JSONObject(str);
            String accessToken = jsonObject.getString("token");
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }




    private static final char HEX_DIGITS[] = {
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F'
    };

    public final static String bin2Hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }


}
