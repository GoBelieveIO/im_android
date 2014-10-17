package cn.ngds.im.demo.view.base;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * BaseActivity
 * Description: 基础Activity
 */
public abstract class BaseActivity extends FragmentActivity {
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onBaseCreate(savedInstanceState);
        initView(savedInstanceState);
        bindView(savedInstanceState);
    }

    /**
     * 必须在此设置一个ContentView，除非它没有界面
     *
     * @param savedInstanceState
     */
    protected abstract void onBaseCreate(Bundle savedInstanceState);

    /**
     * 视图初始化
     * <p/>
     * 处理手势绑定、view和fragment的注入
     *
     * @param savedInstanceState
     */
    protected abstract void initView(Bundle savedInstanceState) ;

    /**
     * 在此处理视图逻辑的绑定
     *
     * @param savedInstanceState
     */
    protected abstract void bindView(Bundle savedInstanceState);


}
