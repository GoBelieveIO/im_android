package com.beetle.bauhinia.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.cjt2325.cameralibrary.JCameraView;
import com.cjt2325.cameralibrary.listener.ClickListener;
import com.cjt2325.cameralibrary.listener.ErrorListener;
import com.cjt2325.cameralibrary.listener.JCameraListener;
import com.cjt2325.cameralibrary.util.DeviceUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.beetle.imkit.R;
public class CameraActivity extends BaseActivity {
    private final String TAG = "goubuli";

    private JCameraView jCameraView;

    private String cameraDir;//录制过程中的保存路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_camera);


        Intent intent = getIntent();
        String dir = intent.getStringExtra("dir");
        if (!TextUtils.isEmpty(dir)) {
            cameraDir = dir;
        } else {
            cameraDir = this.getExternalCacheDir().getPath() + File.separator + "JCamera";
        }

        File file = new File(cameraDir);
        if (!file.exists()) {
            file.mkdirs();
        }

        jCameraView = (JCameraView) findViewById(R.id.jcameraview);
        jCameraView.setSaveVideoPath(cameraDir);
        jCameraView.setFeatures(JCameraView.BUTTON_STATE_BOTH);
        jCameraView.setTip("轻触拍照，按住摄像");
        jCameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_MIDDLE);
        jCameraView.setErrorLisenter(new ErrorListener() {
            @Override
            public void onError() {
                //错误监听
                Log.i(TAG, "camera error");
                Intent intent = new Intent();
                setResult(103, intent);
                finish();
            }

            @Override
            public void AudioPermissionError() {
                Toast.makeText(CameraActivity.this, "给点录音权限可以?", Toast.LENGTH_SHORT).show();
            }
        });
        //JCameraView监听
        jCameraView.setJCameraLisenter(new JCameraListener() {
            @Override
            public void captureSuccess(Bitmap bitmap) {
                //获取图片bitmap
                Log.i(TAG, "bitmap = " + bitmap.getWidth() + " " + bitmap.getHeight());
                long dataTake = System.currentTimeMillis();
                String jpegName = cameraDir + File.separator + "picture_" + dataTake + ".jpg";

                saveBitmap(bitmap, jpegName);
                Intent intent = new Intent();
                intent.putExtra("picture_path", jpegName);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {
                Log.i(TAG, "url = " + url + " length:" + new File(url).length());

                long dataTake = System.currentTimeMillis();
                String jpegName = cameraDir + File.separator + "picture_" + dataTake + ".jpg";
                //获取视频路径
                boolean r = saveBitmap(firstFrame, jpegName);
                Log.i(TAG, "save thumb success:" + jpegName);
                if (r) {
                    Intent intent = new Intent();
                    intent.putExtra("video_path", url);
                    intent.putExtra("thumbnail_path", jpegName);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        jCameraView.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                CameraActivity.this.finish();
            }
        });
        jCameraView.setRightClickListener(new ClickListener() {
            @Override
            public void onClick() {
                Toast.makeText(CameraActivity.this,"Right",Toast.LENGTH_SHORT).show();
            }
        });

        Log.i(TAG, DeviceUtil.getDeviceModel());
    }

    public void storeByteArray(String fileName, ByteArrayOutputStream byteStream) throws IOException {
        File file = new File(fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byteStream.writeTo(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    boolean saveBitmap(Bitmap bitmap, String fileName) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        try {
            storeByteArray(fileName, os);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏显示
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        jCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        jCameraView.onPause();
    }
}
