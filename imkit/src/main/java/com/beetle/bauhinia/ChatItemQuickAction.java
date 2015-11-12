/**
 * 私聊长按弹出操作
 */

package com.beetle.bauhinia;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.beetle.imkit.R;

public class ChatItemQuickAction {
    public enum ChatQuickAction {
        DELETE, FORWARD, RESEND, COPY;

        public String getName(Context context) {
            switch (this) {
                case DELETE:
                    return context.getString(R.string.im_btn_del);
                case FORWARD:
                    return context.getString(R.string.im_btn_forward);
                case RESEND:
                    return context.getString(R.string.im_btn_resend);
                case COPY:
                    return context.getString(R.string.im_btn_copy);
            }
            return "";
        }
    }

    public interface ChatQuickActionResult {
        void onSelect(ChatQuickAction action);
    }

    public static void showAction(Context context, final ChatQuickAction[] actions,
            final ChatQuickActionResult result) {
        if (actions == null || actions.length < 1)
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.im_title_choose));
        CharSequence[] items = new CharSequence[actions.length];
        for (int i = 0; i < actions.length; ++i) {
            items[i] = actions[i].getName(context);
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (result != null) {
                    result.onSelect(actions[item]);
                }
            }
        }).show();
    }
}
