package cn.ngds.im.demo.view.header;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import cn.ngds.im.demo.R;

/**
 * HeaderFragment
 * Description: demo中通用的头部模块
 */
public class HeaderFragment extends Fragment implements View.OnClickListener {
    private ImageButton mBtnLeft;
    private ImageButton mBtnRight;
    private TextView mTvTitle;
    private HeaderButtonClickListener mLeftButtonClickListener;
    private HeaderButtonClickListener mRightButtonClickListener;


    public interface HeaderButtonClickListener {
        public void onHeaderButtonClicked();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        View headerView = inflater.inflate(R.layout.fragment_common_header, container, false);
        mBtnLeft = (ImageButton) headerView.findViewById(R.id.ibtn_left);
        mBtnRight = (ImageButton) headerView.findViewById(R.id.ibtn_right);
        mTvTitle = (TextView) headerView.findViewById(R.id.tv_title);

        return headerView;
    }


    public void setCenterText(String centerText) {
        mTvTitle.setText(centerText);
    }

    public void setCenterText(int stringId) {
        mTvTitle.setText(stringId);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibtn_left:
                if (null != mLeftButtonClickListener) {
                    mLeftButtonClickListener.onHeaderButtonClicked();
                }
                break;
            case R.id.ibtn_right:
                if (null != mRightButtonClickListener) {
                    mRightButtonClickListener.onHeaderButtonClicked();
                }
                break;
        }
    }

    public void setLeftButtonClickListener(HeaderButtonClickListener leftButtonClickListener) {
        this.mLeftButtonClickListener = leftButtonClickListener;
    }

    public void setRightButtonClickListener(HeaderButtonClickListener rightButtonClickListener) {
        this.mRightButtonClickListener = rightButtonClickListener;
    }


    public void showOrHideLeftButton(boolean isShown) {
        mBtnLeft.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    public void showOrHideRightButton(boolean isShown) {
        mBtnRight.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }
}
