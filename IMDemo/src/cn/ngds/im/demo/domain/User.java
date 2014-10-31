package cn.ngds.im.demo.domain;

/**
 * User
 * Description:  用户简易信息
 */
public class User {

    private long userId;
    private final String USER_ID = "user_id";

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

}
