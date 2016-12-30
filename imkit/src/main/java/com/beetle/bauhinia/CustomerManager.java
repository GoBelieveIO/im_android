package com.beetle.bauhinia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.CustomerMessageHandler;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.im.IMService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.database.sqlite.SQLiteDatabase.OPEN_READWRITE;


//只有在打开客服聊天界面的情况下，才会建立一个socket的长链接来收发消息
//退出聊天界面后， 用apns来推送新消息的提醒，从而保证资源消耗的最优化
/* 应用没有用户系统
 * 1.app启动时
 * CustomerManager.getInstance().init(getApplicationContext(), appID, appKey, androidID)
 *
 * if (CustomerManager.getInstance().getClientID() == 0) {
 *     CustomerManager.getInstance().registerClient(name, callback{
 *          //注册成功之后，登录当前用户
 *          CustomerManager.getInstance.login()
 *     })
 * } else {
 *     CustomerManager.getInstance.login()
 * }
 *
 * 2.将推送的devicetoken绑定到当前用户
 * CustomerManager.getInstance.bindDeviceToken(deviceToken, callback)
 *
 * 应用有用户系统
 * 1.app启动时
 * CustomerManager.getInstance().init(getApplicationContext(), appID, appKey, androidID)
 *
 *
 * 2.用户登录后，使用用户的id和用户名称来注册顾客id
 * if (CustomerManager.getInstance().getUid() != 当前uid) {
 *     CustomerManager.getInstance().registerClient(uid, name, avatar, callback {
 *          //注册成功之后，登录当前用户
 *          CustomerManager.getInstance.login()
 *     })
 * } else {
 *     CustomerManager.getInstance().login();
 * }
 *
 * 3.将推送的devicetoken绑定到当前用户
 * CustomerManager.getInstance.bindDeviceToken(deviceToken, callback)
 *
 * 4.用户注销
 * CustomerManager.getInstance().unbindDeviceToken(callback {
 *    CustomerManager.getInstance().unregisterClient();
 * });
 *
 *
 * 如何处理未读消息的提醒
 * app启动的时候调用getunreadmessage获取未读标志，
 * 之后通过推送通道的透传消息来接受未读消息的通知，
 * 透传消息的内容json对象，格式如下：
 * {"xiaowei":{"new":1}}
 */


public class CustomerManager {
    private static int PUSH_UNKNOWN = 0;
    private static int PUSH_GCM = 1;
    private static int PUSH_XIAOMI = 2;
    private static int PUSH_HUAWEI = 3;
    private static int PUSH_XG = 4;
    private static int PUSH_JG = 5;

    private static String URL = "http://api.gobelieve.io";

    private static CustomerManager instance = new CustomerManager();
    public static CustomerManager getInstance() {
        return instance;
    }

    private Context context;


    private long appID;
    private String appKey;
    private String deviceID;

    private String name;
    private String avatar;
    private String uid;


    //推送服务商
    private int pushType;
    private String deviceToken;
    private boolean binded;

    private long clientID;
    private String token;
    private long storeID;
    Handler mainHandler;

    //应用启动时，初始化CustomerManager
    public void init(Context context, long appID, String appKey, String deviceID) {
        this.context = context;

        mainHandler = new Handler(Looper.getMainLooper());
        this.load();

        assert(appID > 0);
        assert (this.appID == 0 || appID == this.appID);

        this.appID = appID;
        this.appKey = appKey;
        this.deviceID = deviceID;
    }

    public long getClientID() {
        return clientID;
    }

    public String getUid() {
        return uid;
    }

    private void load() {
        SharedPreferences customer = context.getSharedPreferences("customer", Context.MODE_PRIVATE);

        this.appID = customer.getLong("appid", 0);
        this.uid = customer.getString("uid", "");
        this.name = customer.getString("name", "");
        this.avatar = customer.getString("avatar", "");
        this.token = customer.getString("token", "");
        this.clientID = customer.getLong("client_id", 0);

        this.pushType = customer.getInt("push_type", PUSH_UNKNOWN);
        this.deviceToken = customer.getString("device_token", "");
        this.binded = customer.getBoolean("binded", false);

        this.storeID = customer.getLong("store_id", 0);
    }

    private void save() {
        SharedPreferences pref = context.getSharedPreferences("customer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putLong("appid", this.appID);
        editor.putString("uid", this.uid != null ? this.uid : "");
        editor.putString("name", (this.name != null ? this.name : ""));
        editor.putString("avatar", this.avatar != null ? this.avatar : "");
        editor.putString("token", this.token != null ? this.token : "");
        editor.putLong("client_id", this.clientID);

        editor.putBoolean("binded", this.binded);
        editor.putString("device_token", this.deviceToken != null ? this.deviceToken : "");
        editor.putInt("push_type", this.pushType);

        editor.putLong("store_id", this.storeID);

        editor.commit();
    }


    public  interface OnRegisterCallback {
        void onSuccess(long clientID);
        void onFailure(int code, String message);
    }

    public void registerClient(String name, OnRegisterCallback callback) {
        registerClient("", name, "", callback);
    }

    //应用自身的用户id和用户信息
    //如果此uid之前未注册，会生成一个新的顾客id,否则返回之前注册获得的顾客id
    //在用户登录之后调用
    //如果注册失败是因为网络波动导致，开发者可尝试多次调用，从而提供注册的成功率
    public void registerClient(final String uid, final String name, final String avatar, final OnRegisterCallback callback) {

        String url = URL + "/customer/register";
        OkHttpClient client = new OkHttpClient();

        JSONObject body = new JSONObject();
        try {
            body.put("customer_id", uid != null ? uid : "");
            body.put("name", (name != null ? name : ""));
            body.put("avatar", (avatar != null ? avatar : ""));
            body.put("appid", this.appID);
            body.put("device_id", this.deviceID);
            body.put("platform_id", 2);
        } catch (JSONException e) {
            e.printStackTrace();
            //impossible
            return;
        }

        String basic = String.format("%d:%s", this.appID, this.appKey);
        byte [] encode = Base64.encode(basic.getBytes(), Base64.NO_WRAP);
        String auth = String.format("Basic %s", new String(encode));

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, body.toString());
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(1000, "注册失败");
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(2000, "注册失败");
                        }
                    });
                    return;
                }

                try {
                    String resp = response.body().string();
                    JSONObject obj = new JSONObject(resp);

                    JSONObject data = obj.getJSONObject("data");
                    final String token = data.getString("token");
                    final long clientID = data.getLong("client_id");
                    final long storeID = data.getLong("store_id");


                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            CustomerManager.this.avatar = avatar;
                            CustomerManager.this.name = name;
                            CustomerManager.this.uid = uid;
                            CustomerManager.this.token = token;
                            CustomerManager.this.clientID = clientID;
                            CustomerManager.this.storeID = storeID;
                            CustomerManager.this.save();

                            callback.onSuccess(clientID);
                        }
                    };
                    CustomerManager.this.mainHandler.post(r);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(1000, "注册失败");
                        }
                    };
                    CustomerManager.this.mainHandler.post(r);
                }
            }
        });
    }

    //清空当前顾客的登录信息
    public void unregisterClient() {
        this.clientID = 0;
        this.token = "";
        this.name = "";
        this.avatar = "";

        this.deviceToken = "";
        this.pushType = PUSH_UNKNOWN;
        this.binded = false;

        this.storeID = 0;

        save();
    }

    private void copyDataBase(String asset, String path) throws IOException {
        InputStream mInput = this.context.getAssets().open(asset);
        OutputStream mOutput = new FileOutputStream(path);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }
    //顾客登录
    public void login() {
        //sqlite
//        try {
//            File p = context.getDir("db_" + this.clientID, context.MODE_PRIVATE);
//            File f = new File(p, "gobelieve.db");
//            String path = f.getPath();
//            if (!f.exists()) {
//                copyDataBase("gobelieve.db", path);
//            }
//            SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null, OPEN_READWRITE, null);
//            CustomerMessageDB.getInstance().setDb(db);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //file
        CustomerMessageDB csDB = CustomerMessageDB.getInstance();
        String path = String.format("customer_%d", this.clientID);
        File dir = this.context.getDir(path, context.MODE_PRIVATE);
        csDB.setDir(dir);

        IMService.getInstance().setCustomerMessageHandler(CustomerMessageHandler.getInstance());
        IMService.getInstance().setToken(this.token);
        IMService.getInstance().setDeviceID(this.deviceID);
        IMHttpAPI.setToken(token);
    }


    public interface OnBindCallback {
        void onSuccess();
        void onFailure(int code, String message);
    }

    public interface OnUnbindCallback {
        void onSuccess();
        void onFailure(int code, String message);
    }

    //绑定信鸽的device token
    public void bindXGDeviceToken(String deviceToken, OnBindCallback callback) {
        bindDeviceToken(deviceToken, PUSH_XG, callback);
    }

    //绑定小米推送的device token
    public void bindXMDeviceToken(String deviceToken, OnBindCallback callback) {
        bindDeviceToken(deviceToken, PUSH_XIAOMI, callback);
    }

    //绑定华为推送的device token
    public void bindHWDeviceToken(String deviceToken, OnBindCallback callback) {
        bindDeviceToken(deviceToken, PUSH_HUAWEI, callback);
    }

    //绑定极光推送的device token
    public void bindJGDeviceToken(String deviceToken, OnBindCallback callback) {
        bindDeviceToken(deviceToken, PUSH_JG, callback);
    }


    //绑定GCM的device token
    public void bindGCMDeviceToken(final String deviceToken, final OnBindCallback callback) {
        bindDeviceToken(deviceToken, PUSH_GCM, callback);
    }


    private Request newUnbindDeviceTokenRequest(String deviceToken, int pushType) {
        String url = URL + "/device/unbind";
        JSONObject body = new JSONObject();
        try {
            if (pushType == PUSH_GCM) {
                body.put("gcm_device_token", deviceToken);
            } else if (pushType == PUSH_XG) {
                body.put("xg_device_token", deviceToken);
            } else if (pushType == PUSH_XIAOMI) {
                body.put("xm_device_token", deviceToken);
            } else if (pushType == PUSH_HUAWEI) {
                body.put("hw_device_token", deviceToken);
            } else if (pushType == PUSH_JG) {
                body.put("jp_device_token", deviceToken);
            } else {
                assert(false);
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            //impossible
            return null;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, body.toString());
        String auth = String.format("Bearer %s", this.token);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .post(requestBody)
                .build();

        return request;
    }

    private Request newBindDeviceTokenRequest(String deviceToken, int pushType) {
        String url = URL + "/device/bind";
        JSONObject body = new JSONObject();
        try {
            if (pushType == PUSH_GCM) {
                body.put("gcm_device_token", deviceToken);
            } else if (pushType == PUSH_XG) {
                body.put("xg_device_token", deviceToken);
            } else if (pushType == PUSH_XIAOMI) {
                body.put("xm_device_token", deviceToken);
            } else if (pushType == PUSH_HUAWEI) {
                body.put("hw_device_token", deviceToken);
            } else if (pushType == PUSH_JG) {
                body.put("jp_device_token", deviceToken);
            } else {
                assert(false);
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            //impossible
            return null;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, body.toString());
        String auth = String.format("Bearer %s", this.token);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .post(requestBody)
                .build();

        return request;
    }

    private void bindDeviceToken(final String deviceToken, final int pushType, final OnBindCallback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = newBindDeviceTokenRequest(deviceToken, pushType);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(2000, "bind fail");
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(2000, "bind fail");
                        }
                    });
                    return;
                }

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        CustomerManager.this.binded = true;
                        CustomerManager.this.deviceToken = deviceToken;
                        CustomerManager.this.pushType = pushType;
                        CustomerManager.this.save();

                        callback.onSuccess();
                    }
                };
                mainHandler.post(r);
            }
        });
    }

    public void unbindDeviceToken(final OnUnbindCallback callback) {
        if (this.clientID == 0) {
            return;
        }

        if (!this.binded) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    CustomerManager.this.binded = false;
                    CustomerManager.this.deviceToken = "";
                    CustomerManager.this.pushType = PUSH_UNKNOWN;
                    CustomerManager.this.save();
                    callback.onSuccess();
                }
            });
            return;
        }

        OkHttpClient client = new OkHttpClient();

        Request request = newBindDeviceTokenRequest(deviceToken, pushType);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(1000, "bind fail");
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(2000, "bind fail");
                        }
                    });
                    return;
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CustomerManager.this.binded = false;
                        CustomerManager.this.deviceToken = "";
                        CustomerManager.this.pushType = PUSH_UNKNOWN;
                        CustomerManager.this.save();
                        callback.onSuccess();
                    }
                });
            }
        });
    }

    public interface OnGetUnreadCallback {
        void onSuccess(boolean hasUnread);
        void onFailure(int code, String message);
    }

    private Request newGetUnreadRequest() {
        String p = String.format("/messages/offline?platform_id=2&device_id=%s", this.deviceID);
        String url = URL + p;
        String auth = String.format("Bearer %s", this.token);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .get()
                .build();

        return request;
    }

    public void getUnreadMessage(final OnGetUnreadCallback callback) {
        if (this.clientID == 0) {
            return;
        }

        Request request = newGetUnreadRequest();
        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final String message = e.getMessage();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(1000, message == null ? "" : message);
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(2000, "get unread failure");
                        }
                    });
                    return;
                }

                try {
                    String resp = response.body().string();
                    JSONObject obj = new JSONObject(resp);

                    JSONObject data = obj.getJSONObject("data");
                    final int hasNew = data.getInt("new");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(hasNew == 1);
                        }
                    };
                    CustomerManager.this.mainHandler.post(r);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(1000, "get unread failure");
                        }
                    };
                    CustomerManager.this.mainHandler.post(r);
                }

            }
        });

    }

    public void startCustomerActivity(Context context, String title) {
        Intent intent = new Intent();
        intent.putExtra("token", this.token);
        intent.putExtra("current_uid", this.clientID);
        intent.putExtra("store_id", this.storeID);
        intent.putExtra("app_id", this.appID);
        intent.putExtra("title", title);
        intent.setClass(context, XWCustomerMessageActivity.class);
        context.startActivity(intent);
    }
}
