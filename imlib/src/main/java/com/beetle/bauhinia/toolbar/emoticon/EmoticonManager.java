package com.beetle.bauhinia.toolbar.emoticon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;

import com.beetle.imlib.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.beetle.bauhinia.toolbar.emoticon.EmoticonUtils.FILE_EMOTICON;

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
    private HashMap<String, Emoticon> mEmoticons = new HashMap<>();

    /**
     * 保存于内存中的表情列表
     */
    private List<Emoticon> mEmoticonList = new ArrayList<>();
    private Context mContext;

    private Set<String> mEmojiSet;

    private int mEmoticonSize;

    public static EmoticonManager getInstance() {
        return InstanceContainer.ISNATNCE;
    }


    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        mEmojiSet = EmoticonUtils.getEmojiEncodeSet();
        mEmoticonSize = EmoticonUtils.getNormalSize(context);

        loadUnicodeEmoji(context);
        if (false) {
            //disable image emoji
            loadImageEmoji(context);
        }

        int pageCount = (int) Math.ceil((double) mEmoticonList.size() / PAGE_EMOTICON_SIZE);
        for (int i = 0; i < pageCount; i++) {
            mEmoticonPageList.add(getPageData(i));
        }
    }

    private void loadUnicodeEmoji(Context context) {
        Map<String, String> emojis = EmoticonUtils.getEmojiMap();

        for (Map.Entry<String, String> e : emojis.entrySet()) {
            String desc = e.getKey();
            String fileName = e.getValue();
            int resId = context.getResources().getIdentifier(fileName, "drawable", context.getPackageName());
            if (resId != 0) {
                Emoticon emoticon = new Emoticon();
                emoticon.setId(resId);
                emoticon.setName(fileName);
                emoticon.setDesc(desc);
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
                bitmap = Bitmap.createScaledBitmap(bitmap, mEmoticonSize, mEmoticonSize, true);
                emoticon.setBitmap(bitmap);
                mEmoticonList.add(emoticon);
                mEmoticons.put(desc, emoticon);
            }
        }
    }

    private void loadImageEmoji(Context context) {
        List<String> emoticonStrList = EmoticonUtils.readFile(context, FILE_EMOTICON);
        //已经加载过数据，或待解析数据集为空，直接返回
        if (emoticonStrList.size() <= 0) {
            return;
        }
        for (String str : emoticonStrList) {
            String[] text = str.split(",");
            String fileName = text[0].substring(0, text[0].lastIndexOf("."));
            String desc = text[1];
            int resId = context.getResources().getIdentifier(fileName, "drawable", context.getPackageName());
            if (resId != 0) {
                Emoticon emoticon = new Emoticon();
                emoticon.setId(resId);
                emoticon.setName(fileName);
                emoticon.setDesc(desc);
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
                bitmap = Bitmap.createScaledBitmap(bitmap, mEmoticonSize, mEmoticonSize, true);
                emoticon.setBitmap(bitmap);
                mEmoticonList.add(emoticon);
                mEmoticons.put(desc, emoticon);
            }
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
        if (mEmojiSet != null && mEmojiSet.contains(text)) {
            text = EmoticonUtils.EmojiCodeToString(Integer.parseInt(text, 16));
        }
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imgId);
        bitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true);
        ImageSpan imageSpan = new ImageSpan(context, bitmap);
        SpannableString spannableString = new SpannableString(text + " ");
        spannableString.setSpan(imageSpan, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    /**
     * 获取包含表情的文本，表情采用自定义大小
     *
     * @param text
     * @return
     */
    public SpannableString getEmoticonStr(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return new SpannableString("");
        }
        String[] regexes = new String[]{REGEX_CONTAIN_EMOTION};
        SpannableString spannableString = new SpannableString(text);
        for (String regex : regexes) {
            dealEmoticon(spannableString, regex);
        }

        return spannableString;
    }

    private void dealEmoticon(SpannableString spannableString,  String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(spannableString);
        while (matcher.find()) {
            String key = matcher.group();
            Emoticon emoticon = mEmoticons.get(key);

            if (emoticon == null) {
                continue;
            }

            @SuppressWarnings("deprecation")
            ImageSpan imageSpan = new ImageSpan(emoticon.getBitmap());
            int end = matcher.start() + key.length();
            spannableString.setSpan(imageSpan, matcher.start(), end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static class InstanceContainer {
        private final static EmoticonManager ISNATNCE = new EmoticonManager();
    }
}
