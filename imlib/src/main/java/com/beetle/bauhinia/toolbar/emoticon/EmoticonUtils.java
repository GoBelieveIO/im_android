package com.beetle.bauhinia.toolbar.emoticon;

import android.content.Context;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final String TAG = "EmoticonUtils";


    public static String emojiResources[] = {
            "1f60a",  "ee_1",
            "1f603",  "ee_2",
            "1f609",  "ee_3",
            "1f62e",  "ee_4",
            "1f60b",  "ee_5",
            "1f60e",  "ee_6",
            "1f621",  "ee_7",
            "1f616",  "ee_8",
            "1f633",  "ee_9",
            "1f61e",  "ee_10",
            "1f62d",  "ee_11",
            "1f610",  "ee_12",
            "1f607",  "ee_13",
            "1f62c",  "ee_14",
            "1f606",  "ee_15",
            "1f631",  "ee_16",
            "1f385",  "ee_17",
            "1f634",  "ee_18",
            "1f615",  "ee_19",
            "1f637",  "ee_20",
            "1f62f",  "ee_21",
            "1f60f",  "ee_22",
            "1f611",  "ee_23",
            "1f496",  "ee_24",
            "1f494",  "ee_25",
            "1f319",  "ee_26",
            "1f31f",  "ee_27",
            "1f31e",  "ee_28",
            "1f308",  "ee_29",
            "1f60d",  "ee_30",
            "1f61a",  "ee_31",
            "1f48b",  "ee_32",
            "1f339",  "ee_33",
            "1f342",  "ee_34",
            "1f44d",  "ee_35",};



    private static final Map<String, String> emojiMap = new HashMap<String, String>();
    private static final Set<String> emojiSet = new HashSet<>();

    static {
        for (int i = 0; i < emojiResources.length; i+=2) {
            emojiMap.put(emojiResources[i], emojiResources[i+1]);
            emojiSet.add(emojiResources[i]);
        }
    }



    private static int EMOJI_CODE_TO_SYMBOL(int x) {
        return ((((0x808080F0 | (x & 0x3F000) >> 4) | (x & 0xFC0) << 10) | (x & 0x1C0000) << 18) | (x & 0x3F) << 24);
    }

    public static String EmojiCodeToString(int x) {
        int sym = EMOJI_CODE_TO_SYMBOL(x);
        byte data[] = {(byte)(sym&0x00ff), (byte)(sym>>8&0x00ff), (byte)(sym>>16&0x00ff), (byte)(sym>>24)};
        try {
            return new String(data, 0, 4, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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





    public static Map<String, String> getEmojiMap() {
            return emojiMap;
    }

    public static Set<String> getEmojiEncodeSet() {
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
