package cn.ngds.im.demo.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DataUtils
 * Description: 用于显示日期,满足不同的展示条件
 */
public class DataUtils {
    private static final long MILLIS_PER_DAY = 86400000L;
    private static final long INTERVAL_IN_MILLISECONDS = 30000L;
    private static SimpleDateFormat mFullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    public static String getTimestampString(long time) {
        long currentTime = System.currentTimeMillis();
        //时间间隔大于一天,显示完整时间信息
        if (currentTime - time > MILLIS_PER_DAY) {
            return mFullDateFormat.format(new Date(time));
        } else {
            return mSimpleDateFormat.format(new Date(time));
        }
    }

    public static boolean isCloseEnough(long lastMsgTime, long currentMsgTime) {
        return currentMsgTime - lastMsgTime < INTERVAL_IN_MILLISECONDS;
    }
}
