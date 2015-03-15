package com.beetle.bauhinia.formatter;

import com.beetle.bauhinia.db.IMessage;

/**
 * Created by tsung on 10/5/14.
 */
public class MessageFormatter {
    public static String messageContentToString(IMessage.MessageContent content) {
        if (content instanceof IMessage.Text) {
            return ((IMessage.Text) content).text;
        } else if (content instanceof IMessage.Image) {
            return "一张图片";
        } else if (content instanceof IMessage.Audio) {
            return "一段语音";
        } else {
            return content.getRaw();
        }
    }
}
