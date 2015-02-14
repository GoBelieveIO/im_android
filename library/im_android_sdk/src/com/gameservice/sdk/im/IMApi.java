package com.gameservice.sdk.im;

import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * IMApi
 * Description: IM SDK的开放APi
 * Author:walker lee
 */
public class IMApi {
    private final String TAG = "imservice";

    private static IMApi instance = new IMApi();

    /**
     * TokenBindTask
     * Description: 绑定token的任务
     */
    public class TokenBindTask extends AsyncTask {
        private String mDeviceToken;
        private String mAccessToken;
        private static final String BIND_COMMAND = "/device/bind";
        private static final String DEVICE_TOKEN = "ng_device_token";
        private static final String TAG = "TokenBindTask";

        public TokenBindTask(String deviceToken, String accessToken) {
            mDeviceToken = deviceToken;
            mAccessToken = accessToken;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            if (TextUtils.isEmpty(mDeviceToken)) {
                Log.d(TAG, "deviceToken is empty");
                return null;
            }
            if (TextUtils.isEmpty(mAccessToken)) {
                Log.d(TAG, "userId is empty");
                return null;
            }
            try {
                bindDeviceToken(mDeviceToken, mAccessToken);
            } catch (Exception e) {
                //handle error
                e.printStackTrace();
            }
            return null;
        }



        public void bindDeviceToken(String deviceToken, String accessToken) {
            Log.i(TAG, "bind device token:" + deviceToken);
            //construct post body json
            URL url = null;
            InputStream is = null;
            OutputStream os = null;
            HttpURLConnection conn = null;
            try {
                url = new URL("http://" + IMService.HOST + BIND_COMMAND);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) {
                    conn.setRequestProperty("Connection", "close");
                }
                conn.setDoInput(true);
                conn.setDoOutput(true);
                byte[] outputInBytes = initContent(deviceToken).getBytes("UTF-8");
                conn.setFixedLengthStreamingMode(outputInBytes.length);
                conn.connect();
                os = conn.getOutputStream();
                os.write(outputInBytes);
                os.flush();
                os.close();
                //do somehting with response
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "bind device token response failure code is:" + responseCode);
                } else {
                    Log.d(TAG, "bind device token response success");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != os) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (null != conn) {
                    conn.disconnect();
                }
            }
        }

        private String initContent(String deviceToken) {
            if (TextUtils.isEmpty(deviceToken)) {
                throw new IllegalArgumentException("deviceToken is null");
            }
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(DEVICE_TOKEN, deviceToken);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jsonObject.toString();
        }
    }




    public static IMApi getInstance() {
        return instance;
    }

    /**
     * 将deviceToken绑定到服务器以接收离线消息
     * @param deviceToken 用户设备token
     */
    public void bindDeviceToken(byte[] deviceToken, String accessToken) {
        String token = bytesToHex(deviceToken);
        new TokenBindTask(token, accessToken).execute();
    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
