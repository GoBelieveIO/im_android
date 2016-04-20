package com.beetle.bauhinia.gallery.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.beetle.bauhinia.gallery.GalleryImage;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

/**
 * Created by hillwind
 */
public class GalleryGridAdapter extends BaseAdapter {

    private final Context context;
    private final int screenWidth;

    private final List<GalleryImage> imagesList;

    public GalleryGridAdapter(Context context, List<GalleryImage> imagesList, int screenWidth) {
        this.context = context;
        this.imagesList = imagesList;
        this.screenWidth = screenWidth;
    }

    @Override
    public int getCount() {
        return imagesList == null ? 0 : imagesList.size();
    }

    @Override
    public GalleryImage getItem(int position) {
        return imagesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GalleryImage ib = getItem(position);
        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(context, R.layout.gallery_activity_gallery_grid_item, null);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.child_image);
            setConvertViewSize(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.imageView.setImageResource(R.drawable.gallery_picture_place_holder);
        }

        viewHolder.imageView.setTag(ib.path);

        if (ib.path.contains(":/")) {
            Picasso.with(context).load(ib.path).into(viewHolder.imageView);
        } else {
            Picasso.with(context).load(new File(ib.path)).into(viewHolder.imageView);
        }
        return convertView;
    }

    private void setConvertViewSize(final View convertView) {
        int height = screenWidth / 3 - 8;
        convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
    }

    public static class ViewHolder {
        public ImageView imageView;
    }

}
