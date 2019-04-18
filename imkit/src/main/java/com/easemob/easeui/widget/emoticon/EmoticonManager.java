package com.easemob.easeui.widget.emoticon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;

import com.beetle.imkit.R;
import com.easemob.easeui.utils.EaseSmileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.easemob.easeui.widget.emoticon.EmoticonUtils.FILE_EMOJI;
import static com.easemob.easeui.widget.emoticon.EmoticonUtils.FILE_EMOJI_AND_EMOTICON;
import static com.easemob.easeui.widget.emoticon.EmoticonUtils.FILE_EMOTICON;

/**
 * Desc.
 *
 * @author chenxj(陈贤靖)
 * @date 2019/3/11
 */
public class EmoticonManager {

    private static final String TAG = "EmoticonManager";

    /**
     * 每一页表情数量
     */
    private static final int PAGE_EMOTICON_SIZE = 31;
    /**
     * 十六进制数据的正则表达式
     */
    private final static String REGEX_HEX = "[0-9a-fA-F]+";
    /**
     * 微信自定义表情的正则表达式, [text]
     */
    private final static String REGEX_CONTAIN_EMOTION = "\\[[^\\]]+\\]";
    /**
     * emoji表情unicode对应的十六进制的正则表达式
     */
    private final static String REGEX_DIVERSE_EMOJI = "[a-fA-F0-9]{5}";
    /**
     * 表情分页的结果集合
     */
    public List<List<Emoticon>> mEmoticonPageList = new ArrayList<>();
    /**
     * 保存于内存中的表情HashMap
     */
    private HashMap<String, String> mEmoticonMap = new HashMap<>();
    /**
     * 保存于内存中的表情列表
     */
    private List<Emoticon> mEmoticonList = new ArrayList<>();
    private Context mContext;

    private HashMap<String, String> mReverseEncodeMap;

    private int mEmoticonSize;

    /**
     * 跟表情大小有强关联性的表情缓存map
     */
    private Map<Integer, Map<String, SpannableString>> mSizeCohesiveCacheMap = new HashMap<>();

    public static EmoticonManager getInstance() {
        return InstanceContainer.ISNATNCE;
    }

    /**
     * 文本预处理，新旧编码与十六进制之间的替换
     *
     * @param context
     * @param text
     * @return
     */
    private static String textPreprocessing(Context context, String text) {
        /**
         * 替换文本中的emoji的旧编码为新编码对应的十六进制，替换时匹配十六进制数据进行替换
         * @see {@link EmoticonManager#getEmoticonStr(Context, String)}
         */
        String unicodeStr = EmoticonUtils.replaceOldEncodeToHex(context, text);
        /**
         * 替换文本中的emoji的新编码为对应的十六进制，替换时匹配十六进制数据进行替换
         * @see {@link EmoticonManager#getEmoticonStr(Context, String)}
         */
        unicodeStr = EmoticonUtils.replaceEmojiToHex(context, unicodeStr);
        return unicodeStr;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        mReverseEncodeMap = EmoticonUtils.getReverseEmojiEncodeMap(context);
        mEmoticonSize = EmoticonUtils.getNormalSize(context);
        try {
            parseEmoticonData(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseEmoticonData(Context context) {
        List<String> emoticonStrList = EmoticonUtils.readFile(context, FILE_EMOTICON);
        List<String> emojiStrList = EmoticonUtils.readFile(context, FILE_EMOJI);
        List<String> emojiAsEmoticonList = EmoticonUtils.readFile(context, FILE_EMOJI_AND_EMOTICON);
        emoticonStrList.addAll(emojiStrList);
        emoticonStrList.addAll(emojiAsEmoticonList);
        //已经加载过数据，或待解析数据集为空，直接返回
        if (mEmoticonPageList.size() > 0 || emoticonStrList.size() <= 0) {
            return;
        }
        Emoticon emoticon = null;
        for (String str : emoticonStrList) {
            String[] text = str.split(",");
            String fileName = text[0].substring(0, text[0].lastIndexOf("."));
            String desc = text[1];
            mEmoticonMap.put(desc, fileName);
            int resId = context.getResources().getIdentifier(fileName, "drawable", context.getPackageName());
            if (resId != 0) {
                emoticon = new Emoticon();
                emoticon.setId(resId);
                emoticon.setName(fileName);
                emoticon.setDesc(desc);
                mEmoticonList.add(emoticon);
            }
        }
        int pageCount = (int) Math.ceil((double) mEmoticonList.size() / PAGE_EMOTICON_SIZE);
        for (int i = 0; i < pageCount; i++) {
            mEmoticonPageList.add(getPageData(i));
        }
    }

    private List<Emoticon> getPageData(int page) {
        int startIndex = page * PAGE_EMOTICON_SIZE;
        int endIndex = startIndex + PAGE_EMOTICON_SIZE;
        if (endIndex > mEmoticonList.size()) {
            endIndex = mEmoticonList.size();
        }
        List<Emoticon> subList = mEmoticonList.subList(startIndex, endIndex);
        List<Emoticon> list = new ArrayList<>(subList);
        //追加删除项
        Emoticon emoticon = new Emoticon();
        emoticon.setId(R.drawable.emoji_item_delete);
        emoticon.setDesc(mContext.getString(R.string.desc_emoticon_delete));
        emoticon.setName(mContext.getString(R.string.name_emoticon_delete));
        list.add(emoticon);
        return list;
    }

    public List<List<Emoticon>> getEmoticonPageList() {
        if ((mEmoticonPageList == null || mEmoticonPageList.size() <= 0) && mContext != null) {
            init(mContext);
        }
        return mEmoticonPageList;
    }

    /**
     * 添加表情
     *
     * @param context
     * @param imgId
     * @param text
     * @return
     */
    public SpannableString addEmoticon(Context context, int imgId, String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        if (mReverseEncodeMap != null && mReverseEncodeMap.containsKey(text)) {
            text = EaseSmileUtils.EmojiCodeToString(Integer.parseInt(text, 16));
        }
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imgId);
        bitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true);
        ImageSpan imageSpan = new ImageSpan(context, bitmap);
        SpannableString spannableString = new SpannableString(text + " ");
        spannableString.setSpan(imageSpan, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    /**
     * 获取包含表情的文本，表情采用默认大小
     *
     * @param context
     * @param text
     * @return
     */
    public SpannableString getEmoticonStr(Context context, CharSequence text) {
        return getEmoticonStr(context, text, mEmoticonSize);
    }

    /**
     * 获取包含表情的文本，表情采用自定义大小
     *
     * @param context
     * @param text
     * @param emoticonSize
     * @return
     */
    public SpannableString getEmoticonStr(Context context, CharSequence text, int emoticonSize) {
        if (TextUtils.isEmpty(text)) {
            return new SpannableString("");
        }
        //有缓存就取缓存数据
        Map<String, SpannableString> cacheMap = mSizeCohesiveCacheMap.get(emoticonSize);
        if (cacheMap != null) {
            SpannableString spannableString = cacheMap.get(text);
            if (spannableString != null) {
                return spannableString;
            }
        } else {
            cacheMap = new HashMap<>();
        }
        //预处理
        text = textPreprocessing(context, text.toString());
        String[] regexes = new String[]{REGEX_CONTAIN_EMOTION, REGEX_DIVERSE_EMOJI};
        SpannableString spannableString = new SpannableString(text);
        for (String regex : regexes) {
            dealEmoticon(context, spannableString, 0, regex, emoticonSize);
        }
        cacheMap.put(text.toString(), spannableString);
        mSizeCohesiveCacheMap.put(emoticonSize, cacheMap);
        return spannableString;
    }

    private void dealEmoticon(Context context, SpannableString spannableString, int start,
                              String regex, int emoticonSize) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(spannableString);
        while (matcher.find()) {
            if (matcher.start() < start) {
                continue;
            }
            String key = matcher.group();
            String value = mEmoticonMap.get(key);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            int resId = context.getResources().getIdentifier(value, "drawable", context.getPackageName());
            if (resId != 0) {
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
                bitmap = Bitmap.createScaledBitmap(bitmap, emoticonSize, emoticonSize, true);
                @SuppressWarnings("deprecation")
                ImageSpan imageSpan = new ImageSpan(bitmap);
                int end = matcher.start() + key.length();
                spannableString.setSpan(imageSpan, matcher.start(), end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (end < spannableString.length()) {
                    dealEmoticon(context, spannableString, end, regex, emoticonSize);
                }
                break;
            }
        }
    }

    private static class InstanceContainer {
        private final static EmoticonManager ISNATNCE = new EmoticonManager();
    }
}
