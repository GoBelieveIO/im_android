package cn.ngds.im.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import cn.ngds.im.demo.domain.UserHelper;
import cn.ngds.im.demo.view.base.BaseActivity;
import cn.ngds.im.demo.view.base.TabFragment;
import cn.ngds.im.demo.view.contacts.ContactsFragment;
import cn.ngds.im.demo.view.header.HeaderFragment;
import cn.ngds.im.demo.view.setting.SettingsFragment;
import com.gameservice.sdk.im.IMMessage;
import com.gameservice.sdk.im.IMPeerMessageHandler;
import com.gameservice.sdk.im.IMService;
import com.gameservice.sdk.im.IMServiceObserver;


public class MainActivity extends BaseActivity implements IMServiceObserver, IMPeerMessageHandler {
    private ContactsFragment mContactsFragment;
    private SettingsFragment mSettingsFragment;
    private Button[] mTabs;
    private TabFragment[] mFragments;
    private HeaderFragment mHeaderFragment;
    private int currentTabIndex;
    private static final int INDEX_NONE = -1;

    private IMService mIMService;

    @Override
    protected void onBaseCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        initTabs();
        mHeaderFragment =
            (HeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fg_header);
    }

    private void initTabs() {
        mTabs = new Button[2];
        mTabs[0] = (Button) findViewById(R.id.btn_contact_list);
        mTabs[1] = (Button) findViewById(R.id.btn_setting);
        mContactsFragment = new ContactsFragment();
        mSettingsFragment = new SettingsFragment();
        mFragments = new TabFragment[] {mContactsFragment, mSettingsFragment};
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
            mContactsFragment)
            .add(R.id.fragment_container, mSettingsFragment).hide(mSettingsFragment).show(
            mContactsFragment)
            .commit();
        mTabs[currentTabIndex].setSelected(true);
    }

    @Override
    protected void bindView(Bundle savedInstanceState) {
        startIMService();
    }

    private void startIMService() {
        mIMService = IMService.getInstance();
        mIMService.setHost("172.25.1.154");
        mIMService.setPort(23000);
        mIMService.setUID(UserHelper.INSTANCE.getUserId());
        mIMService.addObserver(this);
        mIMService.setPeerMessageHandler(this);
        mIMService.start();
    }


    public void onTabClicked(View view) {
        int index = -1;
        switch (view.getId()) {
            case R.id.btn_contact_list:
                index = 0;
                break;

            case R.id.btn_setting:
                index = 1;
                break;
        }

        if (currentTabIndex != index) {
            mTabs[currentTabIndex].setSelected(false);
            mFragments[currentTabIndex].hide();
            getSupportFragmentManager().beginTransaction().hide(mFragments[currentTabIndex]).show(
                mFragments[index]).commit();
            currentTabIndex = index;
            mTabs[currentTabIndex].setSelected(true);
            mFragments[currentTabIndex].show();
        }
    }


    @Override
    public void onConnectState(IMService.ConnectState state) {

    }

    @Override
    public void onPeerMessage(IMMessage msg) {

    }

    @Override
    public void onPeerMessageACK(int msgLocalID, long uid) {

    }

    @Override
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {

    }

    @Override
    public void onPeerMessageFailure(int msgLocalID, long uid) {

    }

    @Override
    public void onReset() {
        //异地登录,下线用户
        mIMService.stop();
    }

    @Override
    public boolean handleMessage(IMMessage msg) {
        return true;
    }

    @Override
    public boolean handleMessageACK(int msgLocalID, long uid) {
        return true;
    }

    @Override
    public boolean handleMessageRemoteACK(int msgLocalID, long uid) {
        return true;
    }

    @Override
    public boolean handleMessageFailure(int msgLocalID, long uid) {
        return true;
    }
}
