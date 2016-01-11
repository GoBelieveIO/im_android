package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface LoginPointObserver {
    //当前用户ID在其它地方登录
    public void onLoginPoint(LoginPoint lp);

}