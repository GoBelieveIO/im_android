package com.beetle.bauhinia.toolbar.emoticon;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.GridLayoutManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.beetle.imlib.R;
import com.beetle.bauhinia.tools.DisplayUtils;
import com.beetle.bauhinia.toolbar.EaseExpandRecylerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Desc.
 *
 * @author chenxj(陈贤靖)
 * @date 2019/3/11
 */
public class EmoticonPanel extends FrameLayout {

    private final static int DEFAULT_COLUMNS = 8;
    /**
     * 指示器圆点半径，选中和未选中，单位:dp
     */
    private final static int HALF_SELECTED_DOT_DISTANCE = 4;
    private final static int HALF_NORMAL_DOT_DISTANCE = 3;
    private Context mContext;
    private EmoticonManager mEmoticonManager;
    private List<List<Emoticon>> mEmoticonPageList;
    private ViewPager mVpEmoticon;
    private LinearLayout mLlIndicator;
    private ArrayList<View> mEmoticonViewList;
    private ArrayList<EmoticonAdapter> mEmoticonAdapterList;
    private ArrayList<View> mIndicatorViewList;
    /**
     * 列数
     */
    private int mColumns;
    /**
     * 当前所在页数
     */
    private int mCurPage;

    private OnItemEmoticonClickListener mOnItemEmoticonClickListener;

    public EmoticonPanel(@NonNull Context context) {
        this(context, null);
    }

    public EmoticonPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        init(context, attrs);
    }

    public void init(Context context, @Nullable AttributeSet attrs) {
        initData(context, attrs);
        initView(context);
        initViewPager();
        //指示器的初始化放在ViewPager初始化之后，因为要根据ViewPager子View数量确定指示器view个数
        initIndicator();
    }

    private void initData(Context context, @Nullable AttributeSet attrs) {
        mContext = context;
        mEmoticonManager = EmoticonManager.getInstance();
        mEmoticonPageList = mEmoticonManager.getEmoticonPageList();
        mEmoticonViewList = new ArrayList<>();
        mEmoticonAdapterList = new ArrayList<EmoticonAdapter>();
        mIndicatorViewList = new ArrayList<>();
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmoticonPanel);
            mColumns = typedArray.getInt(R.styleable.EmoticonPanel_emoticonColumns, DEFAULT_COLUMNS);
            typedArray.recycle();
        } else {
            mColumns = DEFAULT_COLUMNS;
        }
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.emoticon_view, this);
        mVpEmoticon = (ViewPager) findViewById(R.id.vp_emoticon);
        mLlIndicator = (LinearLayout) findViewById(R.id.ll_emoticon_indicator);
    }

    private void initViewPager() {
        for (int i = 0; i < mEmoticonPageList.size(); i++) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_emoticon_page, null);
            EaseExpandRecylerView recylerView = (EaseExpandRecylerView) view.findViewById(R.id.rv_emoticon);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, mColumns);
            recylerView.setLayoutManager(gridLayoutManager);
            EmoticonAdapter emoticonAdapter = new EmoticonAdapter(mContext, mEmoticonPageList.get(i));
            recylerView.setAdapter(emoticonAdapter);
            emoticonAdapter.setOnClickListener(new EmoticonAdapter.OnItemClickListener() {
                @Override
                public void onClick(int position) {
                    Emoticon emoticon = mEmoticonAdapterList.get(mCurPage).getItem(position);
                    if (emoticon == null || mOnItemEmoticonClickListener == null) {
                        return;
                    }
                    if (emoticon.getId() == R.drawable.emoji_item_delete) {
                        mOnItemEmoticonClickListener.onEmoticonDeleted();
                    } else {
                        if (!TextUtils.isEmpty(emoticon.getDesc())) {
                            SpannableString spannableString = mEmoticonManager.addEmoticon(
                                    mContext, emoticon.getId(), emoticon.getDesc());
                            mOnItemEmoticonClickListener.onEmoticonClick(spannableString);
                        }
                    }
                }
            });
            mEmoticonAdapterList.add(emoticonAdapter);
            mEmoticonViewList.add(view);
        }
        mVpEmoticon.setAdapter(new ViewPagerAdapter(mEmoticonViewList));
        mVpEmoticon.setCurrentItem(0);
        mCurPage = 0;
        mVpEmoticon.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurPage = position;
                drawIndicatorViews(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initIndicator() {
        View view;
        int distance = DisplayUtils.dp2px(mContext, HALF_NORMAL_DOT_DISTANCE);
        int length = 2 * distance;
        for (int i = 0; i < mEmoticonViewList.size(); i++) {
            view = new View(mContext);
            view.setBackgroundResource(R.drawable.bg_indicator_dot);
            view.setEnabled(false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(length, length);
            layoutParams.leftMargin = distance;
            layoutParams.rightMargin = distance;
            mLlIndicator.addView(view, layoutParams);
            mIndicatorViewList.add(view);
        }
        drawIndicatorViews(0);
    }

    private void drawIndicatorViews(int index) {
        int selectedDistance = DisplayUtils.dp2px(mContext, HALF_SELECTED_DOT_DISTANCE) * 2;
        int normalDistance = DisplayUtils.dp2px(mContext, HALF_NORMAL_DOT_DISTANCE) * 2;
        int widthAndHeight;
        for (int i = 0; i < mIndicatorViewList.size(); i++) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mIndicatorViewList.get(i).getLayoutParams();
            if (index == i) {
                mIndicatorViewList.get(i).setEnabled(true);
                widthAndHeight = selectedDistance;
            } else {
                widthAndHeight = normalDistance;
                mIndicatorViewList.get(i).setEnabled(false);
            }
            layoutParams.width = widthAndHeight;
            layoutParams.height = widthAndHeight;
            mIndicatorViewList.get(i).setLayoutParams(layoutParams);
        }
    }


    public void setOnItemEmoticonClickListener(OnItemEmoticonClickListener emoticonClickListener) {
        mOnItemEmoticonClickListener = emoticonClickListener;
    }

    public interface OnItemEmoticonClickListener {

        void onEmoticonClick(SpannableString spannableString);

        void onEmoticonDeleted();
    }
}
