package com.beetle.bauhinia.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.beetle.bauhinia.tools.DisplayUtils;

import co.lujun.androidtagview.TagContainerLayout;

public class TagView extends TagContainerLayout {
    public TagView(Context context) {
        this(context, null);
    }

    public TagView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = DisplayUtils.dp2px(getContext(),240);
        if (w > maxWidth) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.getMode(widthMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int childCount = getChildCount();
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int availableW = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int curLineW = 0;
        int lines = childCount == 0 ? 0 : 1;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int dis = childView.getMeasuredWidth() + this.getHorizontalInterval();
            curLineW += dis;
            if (curLineW - getHorizontalInterval() > availableW) {
                lines++;
                curLineW = dis;
            }
        }
        int width = curLineW + getPaddingLeft() + getPaddingRight();
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        if (lines == 1 && width < availableW && widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(width, getMeasuredHeight());
        } else if (maxWidth < (getMeasuredWidth() - getPaddingLeft() - getPaddingRight())) {
            setMeasuredDimension(width, getMeasuredHeight());
        }
    }

}
