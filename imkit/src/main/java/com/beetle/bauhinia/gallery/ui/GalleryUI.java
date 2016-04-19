package com.beetle.bauhinia.gallery.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.gallery.GalleryImage;
import com.beetle.bauhinia.gallery.view.ScrollViewPager;
import com.beetle.imkit.R;

import java.util.ArrayList;

/**
 * Created by hillwind
 */
public class GalleryUI extends BaseActivity {

    private static final String INTENT_EXTRA_KEY_POSITION = "position";
    private static final String INTENT_EXTRA_KEY_IMAGES = "images";
    private static final String INTENT_EXTRA_KEY_IS_ENTER_FROM_GRID = "is_enter_from_grid";

    private ScrollViewPager mViewPager;
    private GalleryAdapter mPagerAdapter;
    private ImageButton ibViewMorePicture;

    private int mPosition;
    private int mTotal;
    private ArrayList<GalleryImage> imagesList;
    private boolean isEnterFromGrid;

    public static Intent getCallingIntent(Context context, ArrayList<GalleryImage> galleryImages, int position, boolean isEnterFromGrid) {
        Intent intent = getCallingIntent(context, galleryImages, position);
        intent.putExtra(INTENT_EXTRA_KEY_IS_ENTER_FROM_GRID, isEnterFromGrid);
        return intent;
    }

    public static Intent getCallingIntent(Context context, ArrayList<GalleryImage> galleryImages, int position) {
        Intent intent = new Intent(context, GalleryUI.class);
        intent.putParcelableArrayListExtra(INTENT_EXTRA_KEY_IMAGES, galleryImages);
        intent.putExtra(INTENT_EXTRA_KEY_POSITION, position);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity_gallery);
        initViews();
        init();
    }

    private void initViews() {
        mViewPager = (ScrollViewPager) findViewById(R.id.imagebrowser_svp_pager);
        ibViewMorePicture = (ImageButton) findViewById(R.id.ib_view_more_picture);
    }

    private void navigateToViewMorePicture() {
        Intent intent = GalleryGridUI.getCallingIntent(this, imagesList, mViewPager.getCurrentItem());
        startActivity(intent);
    }

    private void init() {
        mPosition = getIntent().getIntExtra(INTENT_EXTRA_KEY_POSITION, 0);
        imagesList = getIntent().getParcelableArrayListExtra(INTENT_EXTRA_KEY_IMAGES);
        isEnterFromGrid = getIntent().getBooleanExtra(INTENT_EXTRA_KEY_IS_ENTER_FROM_GRID, false);
        mTotal = imagesList.size();
        if (mPosition > mTotal) {
            mPosition = mTotal - 1;
        }
        mPagerAdapter = new GalleryAdapter(this, imagesList);
        mPagerAdapter.setOnItemClickListener(new GalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ViewGroup container, View view, int position) {
                GalleryUI.this.finish();
            }
        });
        mViewPager.setAdapter(mPagerAdapter);
        if (mTotal > 0) {
            mViewPager.setCurrentItem(mPosition, false);
        }
        if (isEnterFromGrid) {
            hideViewMorePictureButton();
        } else {
            showViewMorePictureButton();
        }
    }

    private void hideViewMorePictureButton() {
        ibViewMorePicture.setVisibility(View.GONE);
    }

    private void showViewMorePictureButton() {
        ibViewMorePicture.setVisibility(View.VISIBLE);
        ibViewMorePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToViewMorePicture();
            }
        });
    }

}
