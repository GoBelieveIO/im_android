package com.beetle.bauhinia;

import android.os.Bundle;

import com.beetle.im.IMService;

/**
 * Created by houxh on 16/10/23.
 */

public class XWCustomerMessageActivity extends CustomerMessageActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IMService.getInstance().start();
    }


    @Override
    protected void onStart() {
        super.onStart();
        IMService.getInstance().enterForeground();
    }

    @Override
    protected void onStop() {
        super.onStop();
        IMService.getInstance().enterBackground();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        IMService.getInstance().stop();
    }
}
