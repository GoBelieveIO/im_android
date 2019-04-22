/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.activity;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.imkit.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MessageFileActivity extends BaseActivity {

    protected static final String TAG = "imservice";

    private String url;
    private String filename;
    private int size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        filename = intent.getStringExtra("filename");
        size = intent.getIntExtra("size", 0);

        TextView tv = (TextView)findViewById(R.id.filename);
        tv.setText(filename);

        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        if (filename.endsWith(".doc") || filename.endsWith("docx")) {
            imageView.setImageResource(R.drawable.word);
        } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            imageView.setImageResource(R.drawable.excel);
        } else if (filename.endsWith(".pdf")) {
            imageView.setImageResource(R.drawable.pdf);
        } else {
            imageView.setImageResource(R.drawable.file);
        }

        if (!FileCache.getInstance().isCached(url)) {
            this.download(url);
        }
    }

    public void onOpen(View v) {
        File f = new File(FileCache.getInstance().getCachedFilePath(url));
        Log.i(TAG, "open file:" + filename + " " + f.getAbsolutePath());
        if (f.exists()) {
            try {
                openFile(this, filename, f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
            }


        }.execute();
    }

    public static void openFile(Context context, String filename, File url) throws IOException {
        // Create URI
        File file = url;
        Uri uri = Uri.fromFile(file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Check what kind of file you are trying to open, by comparing the url with extensions.
        // When the if condition is matched, plugin sets the correct intent (mime) type,
        // so Android knew what application to use to open the file
        if (filename.contains(".doc") || filename.contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword");
        } else if(filename.contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf");
        } else if(filename.contains(".ppt") || filename.contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if(filename.contains(".xls") || filename.contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if(filename.contains(".zip") || filename.contains(".rar")) {
            // WAV audio file
            intent.setDataAndType(uri, "application/x-wav");
        } else if(filename.contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf");
        } else if(filename.contains(".wav") || filename.contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav");
        } else if(filename.contains(".gif")) {
            // GIF file
            intent.setDataAndType(uri, "image/gif");
        } else if(filename.contains(".jpg") || filename.contains(".jpeg") || filename.contains(".png")) {
            // JPG file
            intent.setDataAndType(uri, "image/jpeg");
        } else if(filename.contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain");
        } else if(filename.contains(".3gp") || filename.contains(".mpg") || filename.contains(".mpeg") || filename.contains(".mpe") || filename.contains(".mp4") || filename.contains(".avi")) {
            // Video files
            intent.setDataAndType(uri, "video/*");
        } else {
            //if you want you can also define the intent type for any other file

            //additionally use else clause below, to manage other unknown extensions
            //in this case, Android will show all applications installed on the device
            //so you can choose which application to use
            intent.setDataAndType(uri, "*/*");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


}
