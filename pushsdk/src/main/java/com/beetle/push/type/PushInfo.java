package com.beetle.push.type;

import org.json.JSONObject;

/**
 * 推送消息定义
 *
 * @author 慕容秋 (muroqiu@qq.com)
 *         Create on 14-9-12
 */
public class PushInfo {

    // 不需要pacakgeName打开自身应用
    public static final int PUSH_TYPE_APP_WITHOUT_PACKAGENAME= 1;
    // 下载
    public static final int PUSH_TYPE_DOWNLOAD = 2;
    // 打开网页
    public static final int PUSH_TYPE_WEB = 3;
    // 不需要pacakgeName打开指定页面
    public static final int PUSH_TYPE_ACTIVITY_WITHOUT_PACKAGENAME = 4;
    // 透传消息
    public static final int PUSH_TYPE_DELIVER = 5;
    // 启动应用
    public static final int PUSH_TYPE_APP = 6;
    // 启动应用指定页面
    public static final int PUSH_TYPE_ACTIVITY= 7;

    private int push_type;
    private String title;
    private String content;
    private String stat_image;
    private String logo_path;
    private String logo_url;
    private boolean is_clearable;
    private boolean is_vibrate;
    private boolean is_ring;

    // 下载 弹出框 push_type == 2
    private String popup_title;
    private String popup_content;
    private String popup_image_path;
    private String popup_icon_path;
    private String left_btn_name;
    private String right_btn_name;
    private String down_url;
    private String md5;
    private String file_name;

    // 点击打开网页地址 push_type == 3
    private String web_url;

    // 启动应用 push_type == 1, 4
    private JSONObject app_params;
    private String package_name;
    private String active_page;

    private static final String FIELD_PUSH_TYPE = "push_type";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_STAT_IMAGE = "stat_image";
    private static final String FIELD_LOGO_PATH = "logo_path";
    private static final String FIELD_LOGO_URL = "logo_url";
    private static final String FIELD_IS_CLEARABLE = "is_clearable";
    private static final String FIELD_IS_VIBRATE = "is_vibrate";
    private static final String FIELD_IS_RING = "is_ring";
    private static final String FIELD_POPUP_TITLE = "popup_title";
    private static final String FIELD_POPUP_CONTENT = "popup_content";
    private static final String FIELD_POPUP_IMAGE_PATH = "popup_image_path";
    private static final String FIELD_POPUP_ICON_PATH = "popup_icon_path";
    private static final String FIELD_LEFT_BTN_NAME = "btn1_name";
    private static final String FIELD_RIGHT_BTN_NAME = "btn2_name";
    private static final String FIELD_DOWN_URL = "down_url";
    private static final String FIELD_MD5 = "md5";
    private static final String FIELD_FILE_NAME = "file_name";
    private static final String FIELD_WEB_URL = "web_url";
    private static final String FIELD_APP_PARAMS = "app_params";
    private static final String FIELD_PACKAGE_NAME = "package_name";
    private static final String FIELD_ACTIVE_PAGE = "active_page";

    public PushInfo(JSONObject jsonObject) {
        if (jsonObject != null) {
            push_type = jsonObject.optInt(FIELD_PUSH_TYPE);
            title = jsonObject.optString(FIELD_TITLE);
            content = jsonObject.optString(FIELD_CONTENT);
            stat_image = jsonObject.optString(FIELD_STAT_IMAGE);
            logo_path = jsonObject.optString(FIELD_LOGO_PATH);
            logo_url = jsonObject.optString(FIELD_LOGO_URL);
            is_clearable = jsonObject.optBoolean(FIELD_IS_CLEARABLE, true);
            is_vibrate = jsonObject.optBoolean(FIELD_IS_VIBRATE, false);
            is_ring = jsonObject.optBoolean(FIELD_IS_RING, false);
            popup_title = jsonObject.optString(FIELD_POPUP_TITLE);
            popup_content = jsonObject.optString(FIELD_POPUP_CONTENT);
            popup_image_path = jsonObject.optString(FIELD_POPUP_IMAGE_PATH);
            popup_icon_path = jsonObject.optString(FIELD_POPUP_ICON_PATH);
            left_btn_name = jsonObject.optString(FIELD_LEFT_BTN_NAME);
            right_btn_name = jsonObject.optString(FIELD_RIGHT_BTN_NAME);
            down_url = jsonObject.optString(FIELD_DOWN_URL);
            md5 = jsonObject.optString(FIELD_MD5);
            file_name = jsonObject.optString(FIELD_FILE_NAME);
            web_url = jsonObject.optString(FIELD_WEB_URL);
            package_name = jsonObject.optString(FIELD_PACKAGE_NAME);
            app_params = jsonObject.optJSONObject(FIELD_APP_PARAMS);
            active_page = jsonObject.optString(FIELD_ACTIVE_PAGE);
        }
    }

    /**
     * 获取推送消息类型
     *
     * @return
     */
    public int getType() {
        return push_type;
    }

    /**
     * 获取标题
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取内容
     *
     * @return
     */
    public String getContent() {
        return content;
    }

    /**
     * 是否可清除
     *
     * @return
     */
    public boolean isClearable() {
        return is_clearable;
    }

    /**
     * 是否振动
     *
     * @return
     */
    public boolean isVibrate() {
        return is_vibrate;
    }

    /**
     * 是否有响铃
     *
     * @return
     */
    public boolean isRing() {
        return is_ring;
    }

    /**
     * 获取Web网址
     *
     * @return
     */
    public String getWebUrl() {
        return web_url;
    }

    /**
     * 获取可选参数
     *
     * @return
     */
    public JSONObject getOptionParams() {
        return app_params;
    }

    /**
     * 获取启动应用的包名
     *
     * @return
     */
    public String getPackageName() {
        return package_name;
    }

    /**
     * 获取待打开的指定页面名称
     *
     * @return
     */
    public String getActivity() {
        return active_page;
    }


    public void setPush_type(int push_type) {
        this.push_type = push_type;
    }
}
