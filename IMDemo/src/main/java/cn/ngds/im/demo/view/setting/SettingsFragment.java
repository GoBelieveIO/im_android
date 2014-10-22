package cn.ngds.im.demo.view.setting;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.domain.SettingHelper;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.view.base.TabFragment;
import cn.ngds.im.demo.view.header.HeaderFragment;
import cn.ngds.im.demo.view.login.LoginActivity;

/**
 * SettingsFragment
 * Description:设置页面
 */
public class SettingsFragment extends TabFragment implements View.OnClickListener {
    private HeaderFragment mHeaderFragment;
    private CheckBox mCBNotify;
    private CheckBox mCBSound;
    private CheckBox mCBVibrate;
    private Button mBtnLogout;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        mCBNotify = (CheckBox) view.findViewById(R.id.cb_notification);
        mCBSound = (CheckBox) view.findViewById(R.id.cb_sound);
        mCBVibrate = (CheckBox) view.findViewById(R.id.cb_vibrate);
        RelativeLayout areaNotification =
            (RelativeLayout) view.findViewById(R.id.area_notification);
        RelativeLayout areaSound =
            (RelativeLayout) view.findViewById(R.id.area_sound);
        RelativeLayout areaVibrate =
            (RelativeLayout) view.findViewById(R.id.area_vibrate);
        mBtnLogout = (Button) view.findViewById(R.id.btn_logout);
        areaNotification.setOnClickListener(this);
        areaSound.setOnClickListener(this);
        areaVibrate.setOnClickListener(this);
        mBtnLogout.setOnClickListener(this);
        mHeaderFragment =
            (HeaderFragment) getFragmentManager().findFragmentById(R.id.fg_header);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initCheckBoxes();
    }

    private void initCheckBoxes() {
        SettingHelper.INSTANCE.init();
        mCBNotify.setChecked(SettingHelper.INSTANCE.isCanNotify());
        mCBSound.setChecked(SettingHelper.INSTANCE.hasSound());
        mCBVibrate.setChecked(SettingHelper.INSTANCE.isCanVibrate());
    }

    private void initHeaderFragment() {
        if (null == mHeaderFragment) {
            return;
        }
        mHeaderFragment.setCenterText(R.string.main_settings);
        mHeaderFragment.showOrHideLeftButton(false);
        mHeaderFragment.showOrHideRightButton(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.area_notification:
                SettingHelper.INSTANCE.setCanNotify(!SettingHelper.INSTANCE.isCanNotify());
                mCBNotify.setChecked(SettingHelper.INSTANCE.isCanNotify());
                break;
            case R.id.area_sound:
                SettingHelper.INSTANCE.setHasSound(!SettingHelper.INSTANCE.hasSound());
                mCBSound.setChecked(SettingHelper.INSTANCE.isCanNotify());
                break;
            case R.id.area_vibrate:
                SettingHelper.INSTANCE.setHasSound(!SettingHelper.INSTANCE.isCanVibrate());
                mCBVibrate.setChecked(SettingHelper.INSTANCE.isCanVibrate());
                break;
            case R.id.btn_logout:
                UserHelper.INSTANCE.logout();
                Intent intent = new Intent();
                intent.setClass(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
                break;
        }
    }


    @Override
    public void show() {
        initHeaderFragment();
    }

    @Override
    public void hide() {

    }
}
