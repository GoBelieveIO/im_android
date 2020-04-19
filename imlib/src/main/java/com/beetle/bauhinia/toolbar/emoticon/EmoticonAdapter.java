package com.beetle.bauhinia.toolbar.emoticon;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.beetle.imlib.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Desc.
 *
 * @author chenxj(陈贤靖)
 * @date 2019/3/11
 */
public class EmoticonAdapter extends RecyclerView.Adapter<EmoticonAdapter.BaseViewHolder> {

    private Context mContext;

    private List<Emoticon> mEmoticonList;
    private OnItemClickListener mOnItemClickListener;

    public EmoticonAdapter(Context context, @Nullable List<Emoticon> data) {
        mEmoticonList = data;
        if (mEmoticonList == null) {
            mEmoticonList = new ArrayList<>();
        }
        mContext = context;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_emoticon, parent, false);
        return new BaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, final int position) {
        Emoticon emoticon = mEmoticonList.get(position);
        if (emoticon.getId() == R.drawable.emoji_item_delete) {
            holder.ivEmoticon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.emoji_item_delete));
        } else {
            holder.ivEmoticon.setImageBitmap(emoticon.getBitmap());
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEmoticonList.size();
    }

    public Emoticon getItem(int position) {
        return mEmoticonList.get(position);
    }

    public void setOnClickListener(OnItemClickListener listener) {
        if (listener != null) {
            this.mOnItemClickListener = listener;
        }
    }

    public interface OnItemClickListener {

        void onClick(int position);
    }

    class BaseViewHolder extends RecyclerView.ViewHolder {

        public ImageView ivEmoticon;

        public BaseViewHolder(View itemView) {
            super(itemView);
            ivEmoticon = (ImageView) itemView.findViewById(R.id.iv_emoticon);
        }
    }

}
