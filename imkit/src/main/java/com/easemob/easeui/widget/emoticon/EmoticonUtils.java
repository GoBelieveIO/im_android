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
import java.util.HashSet;
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
    public final static String FILE_EMOTICON = "emoticon";
    public final static String FILE_EMOJI_AND_EMOTICON = "emoji_and_emoticon";
    public final static String FILE_EMOJI = "emoji";
    private static final String TAG = "EmoticonUtils";


    static int emojis[] = {
            0x1f604,
            0x1f637,
            0x1F602,
            0x1F61D,
            0x1f633,
            0x1f631,
            0x1F614,
            0x1f612,
            0x1f47b,
            0x1f64f,
            0x1f4aa,
            0x1f389,
            0x1f381
    };

    public static String emojisHex[] = {
            "1f604",
            "1f637",
            "1F602",
            "1F61D",
            "1f633",
            "1f631",
            "1F614",
            "1f612",
            "1f47b",
            "1f64f",
            "1f4aa",
            "1f389",
            "1f381" 
    };



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
     * 将emoji替换成十六进制编码字符串
     *
     * @param context
     * @param text
     * @see {@link EmoticonManager#getEmoticonStr(Context, String)} 方法中是根据十六进制的正则匹配来替换成图片资源的
     */
    public static String replaceEmojiToHex(String text) {
        //以新编码的十六进制为key
        for (int i = 0; i < emojis.length; i++) {
            String keyHexStr = EaseSmileUtils.EmojiCodeToString(emojis[i]);
            if (text.contains(keyHexStr)) {
                text = text.replace(keyHexStr, emojisHex[i]);
            }
        }
        return text;
    }



    public static Set<String> getEmojiEncodeSet() {
        Set<String> emojiSet = new HashSet<>();
        for (int i = 0; i < emojisHex.length; i++) {
            emojiSet.add(emojisHex[i]);
        }
        return emojiSet;
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
