package com.beetle.bauhinia.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;



public class PhotoActivity extends BaseActivity {
    static final String EXTRA_URL = "im.url";

    ImageView photo;

    public static Intent newIntent(Context context, String url) {
        Intent intent = new Intent();
        intent.setClass(context, PhotoActivity.class);
        intent.putExtra(EXTRA_URL, url);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        try {
            getActionBar().hide();
        } catch (Exception ignored) {}

        photo = (ImageView)findViewById(R.id.photo);
        Picasso.with(this)
                .load(getIntent().getStringExtra(EXTRA_URL))
                .fit()
                .centerInside()
                .into(photo);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }
}
