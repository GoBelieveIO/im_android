/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.imkit.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.io.InputStream;

public class PlayerActivity extends BaseActivity {

    private MediaController mediaController;
    private String videoURL;

    private boolean secret;//是否点对点加密
    private long sender;//点对点加密消息的发送者

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);
        Intent intent = getIntent();

        String url = intent.getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }

        secret = intent.getBooleanExtra("secret", false);
        sender = intent.getLongExtra("sender", -1);

        if (secret && sender == -1) {
            finish();
            return;
        }


        videoURL = url;

        VideoView vv = (VideoView) findViewById(R.id.video);
        mediaController = new MediaController(this);
        vv.setMediaController(mediaController);


        if (!FileCache.getInstance().isCached(videoURL)) {
            download(videoURL);
        } else {
            String path = FileCache.getInstance().getCachedFilePath(videoURL);
            vv.setVideoPath(path);
            //autoplay
            vv.start();
            showControl();
        }
    }

    private void showControl() {
        Handler handler=new Handler();
        Runnable runnable=new Runnable(){
            @Override
            public void run() {
                mediaController.show();
            }
        };
        handler.postDelayed(runnable, 100);
    }

    private void download(final String url) {
        final ProgressDialog dialog = ProgressDialog.show(this, null, "正在下载...");
        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... urls) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        InputStream inputStream = response.body().byteStream();
                        FileCache.getInstance().storeFile(url, inputStream);
                        inputStream.close();
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                dialog.dismiss();
                if (!result) {
                    Toast.makeText(getApplicationContext(), "下载失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                VideoView vv = (VideoView) findViewById(R.id.video);
                String path = FileCache.getInstance().getCachedFilePath(videoURL);
                vv.setVideoPath(path);
                //autoplay
                vv.start();
                showControl();
            }
        }.execute();
    }


}
