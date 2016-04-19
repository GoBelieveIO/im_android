package com.beetle.bauhinia.gallery.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.beetle.bauhinia.gallery.GalleryImage;
import com.beetle.bauhinia.gallery.tool.ImageUtils;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by hillwind
 */
public class GalleryAdapter extends PagerAdapter {

    private OnItemClickListener listener;
    private final Context context;

    private List<GalleryImage> mPhotos = new ArrayList<GalleryImage>();

    public GalleryAdapter(Context context, List<GalleryImage> photos) {
        this.context = context;
        if (photos != null) {
            mPhotos = photos;
        }
    }

    @Override
    public int getCount() {
        return mPhotos.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public View instantiateItem(final ViewGroup container, final int position) {
        final PhotoView photoView = new PhotoView(container.getContext());

        final String path = mPhotos.get(position).path;
        if (path.contains(":/")) {
            Picasso.with(context).load(path).into(photoView);
        } else {
            Picasso.with(context).load(new File(path)).into(photoView);
        }
        container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(container, v, position);
                }
            }
        });
        photoView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                if (listener != null) {
                    listener.onItemClick(container, view, position);
                }
            }
        });
        photoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Dialog dialog = PhotoActionPopup.createDialog(context, new PhotoActionPopup.Listener() {
                    @Override
                    public void onSaveToPhone() {
                        saveImageToPhone(photoView, path);
                    }
                });
                dialog.show();
                return false;
            }
        });
        return photoView;
    }

    private void saveImageToPhone(final PhotoView photoView, final String path) {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
                try {
                    String desImagePath = ImageUtils.savePNGImage(context, path, bitmap);
                    subscriber.onNext(desImagePath);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.gallery_image_save_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(String s) {
                        Toast.makeText(context, context.getString(R.string.gallery_image_saved_to) + s, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(ViewGroup container, View view, int position);
    }

}
