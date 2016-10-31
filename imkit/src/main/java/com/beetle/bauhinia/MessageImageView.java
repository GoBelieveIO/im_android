package com.beetle.bauhinia;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.imkit.R;
import com.squareup.picasso.Picasso;

import java.beans.PropertyChangeEvent;

public class MessageImageView extends MessageRowView {

    protected ProgressBar uploadingProgressBar;
    protected View maskView;

    public MessageImageView(Context context, boolean incomming, boolean isShowUserName) {
        super(context, incomming, isShowUserName);

        int contentLayout = R.layout.chat_content_image;

        ViewGroup group = (ViewGroup)this.findViewById(R.id.content);
        group.addView(inflater.inflate(contentLayout, group, false));
        uploadingProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        if (!incomming) {
            maskView = findViewById(R.id.mask);
        }
    }

    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        ImageView imageView = (ImageView)findViewById(R.id.image);
        Picasso.with(context)
                .load(((IMessage.Image) msg.content).url + "@256w_256h_0c")
                .placeholder(R.drawable.image_download_fail)
                .into(imageView);

        boolean uploading = msg.getUploading();
        if (uploading) {
            if (maskView != null) {
                maskView.setVisibility(View.VISIBLE);
            }
            uploadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            if (maskView != null) {
                maskView.setVisibility(View.GONE);
            }
            uploadingProgressBar.setVisibility(View.GONE);
        }
        requestLayout();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getPropertyName().equals("uploading")) {
            boolean uploading = this.message.getUploading();
            if (uploading) {
                if (maskView != null) {
                    maskView.setVisibility(View.VISIBLE);
                }
                uploadingProgressBar.setVisibility(View.VISIBLE);
            } else {
                if (maskView != null) {
                    maskView.setVisibility(View.GONE);
                }
                uploadingProgressBar.setVisibility(View.GONE);
            }
        }
    }
}