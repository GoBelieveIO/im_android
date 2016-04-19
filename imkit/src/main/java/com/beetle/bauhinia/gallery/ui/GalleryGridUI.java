package com.beetle.bauhinia.gallery.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.gallery.GalleryImage;
import com.beetle.bauhinia.gallery.tool.DisplayUtils;
import com.beetle.imkit.R;

import java.util.ArrayList;

/**
 * Created by hillwind
 */
public class GalleryGridUI extends BaseActivity {

    private static final String INTENT_EXTRA_KEY_IMAGES = "images";
    private static final String INTENT_EXTRA_KEY_POSITION = "position";

    private ArrayList<GalleryImage> imagesList;

    private GridView gridView;
    private GalleryGridAdapter mGridAdapter;

    private int initialPosition = 0;

    public static Intent getCallingIntent(Context context, ArrayList<GalleryImage> imagesList, int position) {
        Intent intent = new Intent(context, GalleryGridUI.class);
        intent.putExtra(INTENT_EXTRA_KEY_POSITION, position);
        intent.putParcelableArrayListExtra(INTENT_EXTRA_KEY_IMAGES, imagesList);
        return intent;
    }

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.gallery_activity_gallery_grid);
        handleIntent(getIntent());
        initView();
    }

    private void handleIntent(Intent intent) {
        imagesList = intent.getParcelableArrayListExtra(INTENT_EXTRA_KEY_IMAGES);
        initialPosition = intent.getIntExtra(INTENT_EXTRA_KEY_POSITION, 0);
    }

    private void initView() {
        gridView = (GridView) this.findViewById(R.id.child_grid);
        mGridAdapter = new GalleryGridAdapter(GalleryGridUI.this, imagesList, DisplayUtils.getScreenWidth(this));
        gridView.setAdapter(mGridAdapter);
        gridView.setSelection(initialPosition);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                navigateToViewImageDetail(position);
            }
        });
    }

    private void navigateToViewImageDetail(int position) {
        Intent intent = GalleryUI.getCallingIntent(this, imagesList, position, true);
        startActivity(intent);
    }

}
