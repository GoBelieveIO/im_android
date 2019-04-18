package com.easemob.easeui.widget.emoticon;

import android.content.Context;
import android.view.WindowManager;

import com.easemob.easeui.utils.EaseSmileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Desc.
 *
 * @author chenxj(陈贤靖)
 * @date 2019/3/11
 */
public class EmoticonUtils {

    /**
     * 表情配置文件名
     */
    public final static String FILE_EMOJI_ENCODE = "emoji_encode";
    public final static String FILE_EMOTICON = "emoticon";
    public final static String FILE_EMOJI_AND_EMOTICON = "emoji_and_emoticon";
    public final static String FILE_EMOJI = "emoji";
    private static final String TAG = "EmoticonUtils";

    /**
     * 读取assets目录下配置文件的配置数据
     *
     * @param context
     * @return
     */
    public static List<String> readEmoticonFile(Context context) {
        return readFile(context, FILE_EMOTICON);
    }

    public static List<String> readEmojiFile(Context context) {
        return readFile(context, FILE_EMOJI);
    }

    public static List<String> readFile(Context context, String fileName) {
        List<String> lineList = new ArrayList<>();
        try {
            InputStream inputStream = context.getResources().getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String str = "";
            while ((str = reader.readLine()) != null) {
                lineList.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineList;
    }

    /**
     * 替换表情的旧unicode编码为新编码对应的十六进制
     *
     * @param context
     * @param text
     * @return
     */
    public static String replaceOldEncodeToHex(Context context, String text) {
        //以旧编码的unicode为key
        HashMap<String, String> oldNewMap = getEmojiEncodeMap(context);
        Set<String> keySet = oldNewMap.keySet();
        char[] charArr = text.toCharArray();
        StringBuffer unicodeStrBuffer = new StringBuffer();
        boolean contains;
        for (int i = 0; i < charArr.length; i++) {
            contains = false;
            int code = charArr[i];
            String encodeStr = Integer.toHexString(code);
            Iterator iterator = keySet.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                if (key.equalsIgnoreCase(encodeStr)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                encodeStr = String.valueOf(charArr[i]);
            } else {
                encodeStr = oldNewMap.get(encodeStr.toUpperCase());
            }
            unicodeStrBuffer.append(encodeStr);
        }
        return unicodeStrBuffer.toString();
    }

    /**
     * 将emoji替换成十六进制编码字符串
     *
     * @param context
     * @param text
     * @see {@link EmoticonManager#getEmoticonStr(Context, String)} 方法中是根据十六进制的正则匹配来替换成图片资源的
     */
    public static String replaceEmojiToHex(Context context, String text) {
        //以新编码的十六进制为key
        HashMap<String, String> newOldMap = getReverseEmojiEncodeMap(context);
        Set<String> keySet = newOldMap.keySet();
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            String keyHexStr = EaseSmileUtils.EmojiCodeToString(Integer.parseInt(key, 16));
            if (text.contains(keyHexStr)) {
                text = text.replace(keyHexStr, key);
            }
        }
        return text;
    }

    private static HashMap<String, String> getEmojiEncodeMap(Context context) {
        List<String> lineStrList = readFile(context, FILE_EMOJI_ENCODE);
        HashMap<String, String> oldNewMap = new HashMap<>();
        String[] oldNewLine;
        for (String line : lineStrList) {
            oldNewLine = line.split(",");
            if (oldNewLine.length == 2) {
                oldNewMap.put(oldNewLine[0], oldNewLine[1]);
            }
        }
        return oldNewMap;
    }

    public static HashMap<String, String> getReverseEmojiEncodeMap(Context context) {
        List<String> lineStrList = readFile(context, FILE_EMOJI_ENCODE);
        HashMap<String, String> newOldMap = new HashMap<>();
        String[] oldNewLine;
        for (String line : lineStrList) {
            oldNewLine = line.split(",");
            if (oldNewLine.length == 2) {
                newOldMap.put(oldNewLine[1], oldNewLine[0]);
            }
        }
        return newOldMap;
    }

    private static int getEmoticonSize(Context context) {
        if (context == null) {
            return 0;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            return wm.getDefaultDisplay().getHeight() * 200 / 1920;
        }
        return 0;
    }

    public static int getNormalSize(Context context) {
        return getEmoticonSize(context);
    }

    public static int getSmallSize(Context context) {
        return getEmoticonSize(context) / 4 * 3;
    }

}
