package cn.ngds.im.demo.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

import java.util.Arrays;
import java.util.List;

public abstract class SimpleListAdapter<T> extends BaseAdapter {

    protected LayoutInflater mInflater;
    private   List<T>        mData;
    protected Context        mContext;

    public List<T> getData() {
        return mData;
    }

    public SimpleListAdapter(Context context, T[] data) {
        this(context, Arrays.asList(data));
    }

    public SimpleListAdapter(Context context, List<T> data) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    public void setData(List<T> data) {
        this.mData = data;
    }

    public void setData(T[] data) {
        setData(Arrays.asList(data));
    }

    public void appendData(List<T> data) {
        this.mData.addAll(data);
    }

    public void appendData(T[] data) {
        appendData(Arrays.asList(data));
    }

    protected final LayoutInflater getInflater() {
        return mInflater;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return null == mData ? 0 : mData.size();
    }

    @Override
    public T getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
