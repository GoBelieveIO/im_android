package io.gobelieve.im.demo;

/**
 * Created by houxh on 15/8/8.
 */
public class AppGroupMessageActivity extends com.beetle.bauhinia.GroupMessageActivity {
    protected String getUserName(long uid) {
        return "用户ID:" + uid;
    }
}

