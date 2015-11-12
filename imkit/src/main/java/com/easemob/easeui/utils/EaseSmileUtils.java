/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.easemob.easeui.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.ImageSpan;

import com.beetle.imkit.R;

public class EaseSmileUtils {
	public static final String ee_1 = "[):]";
	public static final String ee_2 = "[:D]";
	public static final String ee_3 = "[;)]";
	public static final String ee_4 = "[:-o]";
	public static final String ee_5 = "[:p]";
	public static final String ee_6 = "[(H)]";
	public static final String ee_7 = "[:@]";
	public static final String ee_8 = "[:s]";
	public static final String ee_9 = "[:$]";
	public static final String ee_10 = "[:(]";
	public static final String ee_11 = "[:'(]";
	public static final String ee_12 = "[:|]"; 
	public static final String ee_13 = "[(a)]";
	public static final String ee_14 = "[8o|]";
	public static final String ee_15 = "[8-|]";
	public static final String ee_16 = "[+o(]";
	public static final String ee_17 = "[<o)]";
	public static final String ee_18 = "[|-)]";
	public static final String ee_19 = "[*-)]";
	public static final String ee_20 = "[:-#]";
	public static final String ee_21 = "[:-*]";
	public static final String ee_22 = "[^o)]";
	public static final String ee_23 = "[8-)]";
	public static final String ee_24 = "[(|)]";
	public static final String ee_25 = "[(u)]";
	public static final String ee_26 = "[(S)]";
	public static final String ee_27 = "[(*)]";
	public static final String ee_28 = "[(#)]";
	public static final String ee_29 = "[(R)]";
	public static final String ee_30 = "[({)]";
	public static final String ee_31 = "[(})]";
	public static final String ee_32 = "[(k)]";
	public static final String ee_33 = "[(F)]";
	public static final String ee_34 = "[(W)]";
	public static final String ee_35 = "[(D)]";


    public static final String ee_unicode_1 = EmojiCodeToString(0x1F60a);
    public static final String ee_unicode_2 = EmojiCodeToString(0x1F603);
	public static final String ee_unicode_3 = EmojiCodeToString(0x1F609);
	public static final String ee_unicode_4 = EmojiCodeToString(0x1F62e);
	public static final String ee_unicode_5 = EmojiCodeToString(0x1F60b);
	public static final String ee_unicode_6 = EmojiCodeToString(0x1F60e);
	public static final String ee_unicode_7 = EmojiCodeToString(0x1F621);
	public static final String ee_unicode_8 = EmojiCodeToString(0x1F616);
	public static final String ee_unicode_9 = EmojiCodeToString(0x1F633);
    public static final String ee_unicode_10 = EmojiCodeToString(0x1F61e); 
	public static final String ee_unicode_11 = EmojiCodeToString(0x1F62d); 
	public static final String ee_unicode_12 = EmojiCodeToString(0x1F610); 
	public static final String ee_unicode_13 = EmojiCodeToString(0x1F607); 
	public static final String ee_unicode_14 = EmojiCodeToString(0x1F62c); 
	public static final String ee_unicode_15 = EmojiCodeToString(0x1F606); 
	public static final String ee_unicode_16 = EmojiCodeToString(0x1F631); 
	public static final String ee_unicode_17 = EmojiCodeToString(0x1F385); 
	public static final String ee_unicode_18 = EmojiCodeToString(0x1F634); 
	public static final String ee_unicode_19 = EmojiCodeToString(0x1F615); 
	public static final String ee_unicode_20 = EmojiCodeToString(0x1F637); 
	public static final String ee_unicode_21 = EmojiCodeToString(0x1F62f); 
	public static final String ee_unicode_22 = EmojiCodeToString(0x1F60f); 
	public static final String ee_unicode_23 = EmojiCodeToString(0x1F611); 
	public static final String ee_unicode_24 = EmojiCodeToString(0x1F496); 
	public static final String ee_unicode_25 = EmojiCodeToString(0x1F494); 
	public static final String ee_unicode_26 = EmojiCodeToString(0x1F319); 
	public static final String ee_unicode_27 = EmojiCodeToString(0x1f31f); 
	public static final String ee_unicode_28 = EmojiCodeToString(0x1f31e); 
	public static final String ee_unicode_29 = EmojiCodeToString(0x1F308); 
	public static final String ee_unicode_30 = EmojiCodeToString(0x1F60d); 
	public static final String ee_unicode_31 = EmojiCodeToString(0x1F61a); 
	public static final String ee_unicode_32 = EmojiCodeToString(0x1F48b); 
	public static final String ee_unicode_33 = EmojiCodeToString(0x1F339); 
	public static final String ee_unicode_34 = EmojiCodeToString(0x1F342); 
	public static final String ee_unicode_35 = EmojiCodeToString(0x1F44d);

    private static final Map<String, String> emojiMap = new HashMap<String, String>();

    static {
        emojiMap.put(ee_1, ee_unicode_1);
        emojiMap.put(ee_2, ee_unicode_2);
        emojiMap.put(ee_3, ee_unicode_3);
        emojiMap.put(ee_4, ee_unicode_4);
        emojiMap.put(ee_5, ee_unicode_5);
        emojiMap.put(ee_6, ee_unicode_6);
        emojiMap.put(ee_7, ee_unicode_7);
        emojiMap.put(ee_8, ee_unicode_8);
        emojiMap.put(ee_9, ee_unicode_9);
        emojiMap.put(ee_10, ee_unicode_10);
        emojiMap.put(ee_11, ee_unicode_11);
        emojiMap.put(ee_12, ee_unicode_12);
        emojiMap.put(ee_13, ee_unicode_13);
        emojiMap.put(ee_14, ee_unicode_14);
        emojiMap.put(ee_15, ee_unicode_15);
        emojiMap.put(ee_16, ee_unicode_16);
        emojiMap.put(ee_17, ee_unicode_17);
        emojiMap.put(ee_18, ee_unicode_18);
        emojiMap.put(ee_19, ee_unicode_19);
        emojiMap.put(ee_20, ee_unicode_20);
        emojiMap.put(ee_21, ee_unicode_21);
        emojiMap.put(ee_22, ee_unicode_22);
        emojiMap.put(ee_23, ee_unicode_23);
        emojiMap.put(ee_24, ee_unicode_24);
        emojiMap.put(ee_25, ee_unicode_25);
        emojiMap.put(ee_26, ee_unicode_26);
        emojiMap.put(ee_27, ee_unicode_27);
        emojiMap.put(ee_28, ee_unicode_28);
        emojiMap.put(ee_29, ee_unicode_29);
        emojiMap.put(ee_30, ee_unicode_30);
        emojiMap.put(ee_31, ee_unicode_31);
        emojiMap.put(ee_32, ee_unicode_32);
        emojiMap.put(ee_33, ee_unicode_33);
        emojiMap.put(ee_34, ee_unicode_34);
        emojiMap.put(ee_35, ee_unicode_35);
    }


    private static final Factory spannableFactory = Spannable.Factory
	        .getInstance();
	
	private static final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();
	
	private static int simlesSize = 0;

	static {
		
	    addPattern(emoticons, ee_1, R.drawable.ee_1);
	    addPattern(emoticons, ee_2, R.drawable.ee_2);
	    addPattern(emoticons, ee_3, R.drawable.ee_3);
	    addPattern(emoticons, ee_4, R.drawable.ee_4);
	    addPattern(emoticons, ee_5, R.drawable.ee_5);
	    addPattern(emoticons, ee_6, R.drawable.ee_6);
	    addPattern(emoticons, ee_7, R.drawable.ee_7);
	    addPattern(emoticons, ee_8, R.drawable.ee_8);
	    addPattern(emoticons, ee_9, R.drawable.ee_9);
	    addPattern(emoticons, ee_10, R.drawable.ee_10);
	    addPattern(emoticons, ee_11, R.drawable.ee_11);
	    addPattern(emoticons, ee_12, R.drawable.ee_12);
	    addPattern(emoticons, ee_13, R.drawable.ee_13);
	    addPattern(emoticons, ee_14, R.drawable.ee_14);
	    addPattern(emoticons, ee_15, R.drawable.ee_15);
	    addPattern(emoticons, ee_16, R.drawable.ee_16);
	    addPattern(emoticons, ee_17, R.drawable.ee_17);
	    addPattern(emoticons, ee_18, R.drawable.ee_18);
	    addPattern(emoticons, ee_19, R.drawable.ee_19);
	    addPattern(emoticons, ee_20, R.drawable.ee_20);
	    addPattern(emoticons, ee_21, R.drawable.ee_21);
	    addPattern(emoticons, ee_22, R.drawable.ee_22);
	    addPattern(emoticons, ee_23, R.drawable.ee_23);
	    addPattern(emoticons, ee_24, R.drawable.ee_24);
	    addPattern(emoticons, ee_25, R.drawable.ee_25);
	    addPattern(emoticons, ee_26, R.drawable.ee_26);
	    addPattern(emoticons, ee_27, R.drawable.ee_27);
	    addPattern(emoticons, ee_28, R.drawable.ee_28);
	    addPattern(emoticons, ee_29, R.drawable.ee_29);
	    addPattern(emoticons, ee_30, R.drawable.ee_30);
	    addPattern(emoticons, ee_31, R.drawable.ee_31);
	    addPattern(emoticons, ee_32, R.drawable.ee_32);
	    addPattern(emoticons, ee_33, R.drawable.ee_33);
	    addPattern(emoticons, ee_34, R.drawable.ee_34);
	    addPattern(emoticons, ee_35, R.drawable.ee_35);
	    
	    simlesSize = emoticons.size();
	}

    private static int EMOJI_CODE_TO_SYMBOL(int x) {
        return ((((0x808080F0 | (x & 0x3F000) >> 4) | (x & 0xFC0) << 10) | (x & 0x1C0000) << 18) | (x & 0x3F) << 24);
    }

    private static String EmojiCodeToString(int x) {
        int sym = EMOJI_CODE_TO_SYMBOL(x);
        byte data[] = {(byte)(sym&0x00ff), (byte)(sym>>8&0x00ff), (byte)(sym>>16&0x00ff), (byte)(sym>>24)};
        try {
            return new String(data, 0, 4, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

	private static void addPattern(Map<Pattern, Integer> map, String smile,
	        int resource) {
	    map.put(Pattern.compile(Pattern.quote(smile)), resource);
	}

	/**
	 * replace existing spannable with smiles
	 * @param context
	 * @param spannable
	 * @return
	 */
	public static boolean addSmiles(Context context, Spannable spannable) {
	    boolean hasChanges = false;
	    for (Entry<Pattern, Integer> entry : emoticons.entrySet()) {
	        Matcher matcher = entry.getKey().matcher(spannable);
	        while (matcher.find()) {
	            boolean set = true;
	            for (ImageSpan span : spannable.getSpans(matcher.start(),
	                    matcher.end(), ImageSpan.class))
	                if (spannable.getSpanStart(span) >= matcher.start()
	                        && spannable.getSpanEnd(span) <= matcher.end())
	                    spannable.removeSpan(span);
	                else {
	                    set = false;
	                    break;
	                }
	            if (set) {
	                hasChanges = true;
	                spannable.setSpan(new ImageSpan(context, entry.getValue()),
	                        matcher.start(), matcher.end(),
	                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            }
	        }
	    }
	    return hasChanges;
	}

	public static Spannable getSmiledText(Context context, CharSequence text) {
	    Spannable spannable = spannableFactory.newSpannable(text);
	    addSmiles(context, spannable);
	    return spannable;
	}

    public static String getSmiledUnicodeText(Context context, CharSequence text) {
        if (emojiMap.containsKey(text)) {
            return emojiMap.get(text);
        } else {
            return "";
        }
    }
	
	public static boolean containsKey(String key){
		boolean b = false;
		for (Entry<Pattern, Integer> entry : emoticons.entrySet()) {
	        Matcher matcher = entry.getKey().matcher(key);
	        if (matcher.find()) {
	        	b = true;
	        	break;
	        }
		}
		
		return b;
	}
	
	public static int getSmilesSize(){
        return simlesSize;
    }
    
	
}
