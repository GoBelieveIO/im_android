package com.beetle.bauhinia.toolbar.emoticon;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Desc.
 *
 * @author chenxj(陈贤靖)
 * @date 2019/3/11
 */
public class ViewPagerAdapter extends PagerAdapter {

    private List<View> mPageViewList;

    public ViewPagerAdapter(List<View> pageViewList) {
        this.mPageViewList = pageViewList;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return super.getItemPosition(object);
    }

    @Override
    public int getCount() {
        return mPageViewList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView(mPageViewList.get(position));
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        container.addView(mPageViewList.get(position));
        return mPageViewList.get(position);
    }
}
