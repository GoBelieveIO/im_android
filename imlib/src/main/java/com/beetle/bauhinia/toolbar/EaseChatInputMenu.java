/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.toolbar;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.*;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.beetle.imlib.R;
import com.beetle.bauhinia.toolbar.EaseChatExtendMenu.EaseChatExtendMenuItemClickListener;
import com.beetle.bauhinia.toolbar.emoticon.EmoticonPanel;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.MentionSpanConfig;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizerConfig;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 聊天页面底部的聊天输入菜单栏 <br/>
 * 主要包含3个控件:EaseChatPrimaryMenu(主菜单栏，包含文字输入、发送等功能), <br/>
 * EaseChatExtendMenu(扩展栏，点击加号按钮出来的小宫格的菜单栏), <br/>
 * 以及EaseEmojiconMenu(表情栏)
 */
public class
EaseChatInputMenu extends LinearLayout implements View.OnClickListener, QueryTokenReceiver {
    FrameLayout primaryMenuContainer;
    protected RelativeLayout emojiconMenuContainer;
    protected EaseChatPrimaryMenu chatPrimaryMenu;
    private EmoticonPanel mEmoticonPanel;
    protected EaseChatExtendMenu chatExtendMenu;
    protected FrameLayout chatExtendMenuContainer;
    protected LayoutInflater layoutInflater;

    private EditText mEtSend;

    private Handler handler = new Handler();
    private ChatInputMenuListener listener;

    public EaseChatInputMenu(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    public EaseChatInputMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EaseChatInputMenu(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.ease_widget_chat_input_menu, this);
        primaryMenuContainer = (FrameLayout) findViewById(R.id.primary_menu_container);
        emojiconMenuContainer = (RelativeLayout) findViewById(R.id.menu_container);
        chatExtendMenuContainer = (FrameLayout) findViewById(R.id.extend_menu_container);
         // 扩展按钮栏
        chatExtendMenu = (EaseChatExtendMenu) findViewById(R.id.extend_menu);

        chatPrimaryMenu = (EaseChatPrimaryMenu) primaryMenuContainer.findViewById(R.id.primary_menu);
        mEtSend = chatPrimaryMenu.editText;
        mEmoticonPanel = new EmoticonPanel(context);
        emojiconMenuContainer.addView(mEmoticonPanel);

        chatPrimaryMenu.buttonSend.setOnClickListener(this);
        chatPrimaryMenu.buttonSetModeKeyboard.setOnClickListener(this);
        chatPrimaryMenu.buttonSetModeVoice.setOnClickListener(this);
        chatPrimaryMenu.buttonMore.setOnClickListener(this);
        chatPrimaryMenu.faceLayout.setOnClickListener(this);
        chatPrimaryMenu.editText.setOnClickListener(this);
        chatPrimaryMenu.editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    chatPrimaryMenu.edittext_layout.setBackgroundResource(R.drawable.ease_input_bar_bg_active);
                } else {
                    chatPrimaryMenu.edittext_layout.setBackgroundResource(R.drawable.ease_input_bar_bg_normal);
                }

                if (listener != null) {
                    listener.onFocusChanged(hasFocus);
                }
            }
        });

        // 监听文字框
        chatPrimaryMenu.editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    chatPrimaryMenu.buttonMore.setVisibility(View.GONE);
                    chatPrimaryMenu.buttonSend.setVisibility(View.VISIBLE);
                } else {
                    chatPrimaryMenu.buttonMore.setVisibility(View.VISIBLE);
                    chatPrimaryMenu.buttonSend.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



        WordTokenizerConfig tokenizerConfig = new WordTokenizerConfig.Builder().build();
        WordTokenizer tokenizer = new WordTokenizer(tokenizerConfig);
        chatPrimaryMenu.editText.setTokenizer(tokenizer);
        MentionSpanConfig.Builder configBuilder = new MentionSpanConfig.Builder();
        configBuilder.setMentionTextColor(Color.BLACK);
        configBuilder.setMentionTextBackgroundColor(Color.TRANSPARENT);
        MentionSpanConfig config = configBuilder.build();
        chatPrimaryMenu.editText.setMentionSpanConfig(config);
        chatPrimaryMenu.editText.setAvoidPrefixOnTap(true);
        chatPrimaryMenu.editText.setQueryTokenReceiver(this);


        chatPrimaryMenu.buttonPressToSpeak.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(listener != null){
                    return listener.onPressToSpeakBtnTouch(v, event);
                }
                return false;
            }
        });

        processEmoticon();
    }

    /**
     * init view 此方法需放在registerExtendMenuItem后面及setCustomEmojiconMenu，
     * setCustomPrimaryMenu(如果需要自定义这两个menu)后面
     */
    public void init() {
        // 初始化extendmenu
        chatExtendMenu.init();
    }

    public void disableSend() {
        hideKeyboard();
        chatPrimaryMenu.buttonMore.setEnabled(false);
        chatPrimaryMenu.buttonSend.setEnabled(false);
        chatPrimaryMenu.buttonPressToSpeak.setEnabled(false);
        chatPrimaryMenu.buttonSetModeVoice.setEnabled(false);
        chatPrimaryMenu.buttonSetModeKeyboard.setEnabled(false);
        chatPrimaryMenu.faceLayout.setEnabled(false);
        chatPrimaryMenu.editText.setEnabled(false);
    }

    public void enableSend() {
        chatPrimaryMenu.buttonMore.setEnabled(true);
        chatPrimaryMenu.buttonSend.setEnabled(true);
        chatPrimaryMenu.buttonPressToSpeak.setEnabled(true);
        chatPrimaryMenu.buttonSetModeVoice.setEnabled(true);
        chatPrimaryMenu.buttonSetModeKeyboard.setEnabled(true);
        chatPrimaryMenu.faceLayout.setEnabled(true);
        chatPrimaryMenu.editText.setEnabled(true);
    }

    public void clearFocus() {
        chatPrimaryMenu.editText.clearFocus();
    }

    public void atUser(long uid, String name) {
        Contact c = new Contact(uid, name);
        chatPrimaryMenu.editText.insertMention(c);
    }

    /**
     * 点击事件
     * @param view
     */
    @Override
    public void onClick(View view){
        int id = view.getId();
        if (id == R.id.btn_send) {
            if(listener != null){
                String ss = chatPrimaryMenu.editText.getText().toString();
                List<MentionSpan> mentions = chatPrimaryMenu.editText.getMentionsText().getMentionSpans();
                Set<Long> atSet = new HashSet<>();
                ArrayList<Long> atList = new ArrayList<>();
                ArrayList<String> atNames = new ArrayList<>();
                for(int i = 0; i < mentions.size(); i++) {
                    Contact c = (Contact)mentions.get(i).getMention();
                    if (!atSet.contains(c.getUid())) {
                        atList.add(c.getUid());
                        atNames.add(c.getName());
                        atSet.add(c.getUid());
                    }
                }
                chatPrimaryMenu.editText.setText("");
                listener.onSendMessage(ss, atList, atNames);
            }
        } else if (id == R.id.btn_set_mode_voice) {
            chatPrimaryMenu.setModeVoice();
            chatPrimaryMenu.showNormalFaceImage();
            hideExtendMenuContainer();
        } else if (id == R.id.btn_set_mode_keyboard) {
            chatPrimaryMenu.setModeKeyboard();
            chatPrimaryMenu.showNormalFaceImage();
            hideExtendMenuContainer();
        } else if (id == R.id.btn_more) {
            chatPrimaryMenu.buttonSetModeVoice.setVisibility(View.VISIBLE);
            chatPrimaryMenu.buttonSetModeKeyboard.setVisibility(View.GONE);
            chatPrimaryMenu.edittext_layout.setVisibility(View.VISIBLE);
            chatPrimaryMenu.buttonPressToSpeak.setVisibility(View.GONE);
            chatPrimaryMenu.showNormalFaceImage();
            toggleMore();
        } else if (id == R.id.et_sendmessage) {
            chatPrimaryMenu.edittext_layout.setBackgroundResource(R.drawable.ease_input_bar_bg_active);
            chatPrimaryMenu.faceNormal.setVisibility(View.VISIBLE);
            chatPrimaryMenu.faceChecked.setVisibility(View.INVISIBLE);
            hideExtendMenuContainer();
        } else if (id == R.id.rl_face) {
            chatPrimaryMenu.toggleFaceImage();
            toggleEmojicon();
        }
    }


    /**
     * 注册扩展菜单的item
     * 
     * @param name
     *            item名字
     * @param drawableRes
     *            item背景
     * @param itemId
     *            id
     * @param listener
     *            item点击事件
     */
    public void registerExtendMenuItem(String name, int drawableRes, int itemId,
            EaseChatExtendMenuItemClickListener listener) {
        chatExtendMenu.registerMenuItem(name, drawableRes, itemId, listener);
    }

    /**
     * 注册扩展菜单的item
     * 
     * @param nameRes
     *            item名字
     * @param drawableRes
     *            item背景
     * @param itemId
     *            id
     * @param listener
     *            item点击事件
     */
    public void registerExtendMenuItem(int nameRes, int drawableRes, int itemId,
            EaseChatExtendMenuItemClickListener listener) {
        chatExtendMenu.registerMenuItem(nameRes, drawableRes, itemId, listener);
    }




    protected void processEmoticon() {
        mEmoticonPanel.setOnItemEmoticonClickListener(new EmoticonPanel.OnItemEmoticonClickListener() {
            @Override
            public void onEmoticonClick(SpannableString spannableString) {
                int index = mEtSend.getSelectionStart();
                Editable editable = mEtSend.getEditableText();
                editable.insert(index, spannableString);
                mEtSend.requestFocus();
            }

            @Override
            public void onEmoticonDeleted() {
                if (!TextUtils.isEmpty(mEtSend.getText())) {
                    KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL,
                            0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                    mEtSend.dispatchKeyEvent(event);
                }
            }
        });
    }

    @Override
    public List<String> onQueryReceived(final @NonNull QueryToken queryToken) {
        if (listener != null && queryToken.getTokenString().equals("@")) {
            listener.onAt();
        }
        return null;
    }

    /**
     * 显示或隐藏图标按钮页
     * 
     */
    protected void toggleMore() {
        if (chatExtendMenuContainer.getVisibility() == View.GONE) {
            hideKeyboard();
            handler.postDelayed(new Runnable() {
                public void run() {
                    chatExtendMenuContainer.setVisibility(View.VISIBLE);
                    chatExtendMenu.setVisibility(View.VISIBLE);
                    emojiconMenuContainer.setVisibility(View.GONE);
                }
            }, 50);
        } else {
            if (emojiconMenuContainer.getVisibility() == View.VISIBLE) {
                emojiconMenuContainer.setVisibility(View.GONE);
                chatExtendMenu.setVisibility(View.VISIBLE);
            } else {
                chatExtendMenuContainer.setVisibility(View.GONE);
                chatPrimaryMenu.showKeyboard();
            }
        }
    }

    /**
     * 显示或隐藏表情页
     */
    protected void toggleEmojicon() {
        if (chatExtendMenuContainer.getVisibility() == View.GONE) {
            hideKeyboard();
            handler.postDelayed(new Runnable() {
                public void run() {
                    chatExtendMenuContainer.setVisibility(View.VISIBLE);
                    chatExtendMenu.setVisibility(View.GONE);
                    emojiconMenuContainer.setVisibility(View.VISIBLE);
                }
            }, 50);
        } else {
            if (emojiconMenuContainer.getVisibility() == View.VISIBLE) {
                chatExtendMenuContainer.setVisibility(View.GONE);
                emojiconMenuContainer.setVisibility(View.GONE);
                chatPrimaryMenu.showKeyboard();
            } else {
                chatExtendMenu.setVisibility(View.GONE);
                emojiconMenuContainer.setVisibility(View.VISIBLE);
            }
        }
    }


    /**
     * 隐藏软键盘
     */
    public void hideKeyboard() {
        chatPrimaryMenu.hideKeyboard();

    }


    /**
     * 隐藏整个扩展按钮栏(包括表情栏)
     */
    public void hideExtendMenuContainer() {
        chatExtendMenu.setVisibility(View.GONE);
        emojiconMenuContainer.setVisibility(View.GONE);
        chatExtendMenuContainer.setVisibility(View.GONE);
        chatPrimaryMenu.showNormalFaceImage();
    }

    /**
     * 系统返回键被按时调用此方法
     * 
     * @return 返回false表示返回键时扩展菜单栏时打开状态，true则表示按返回键时扩展栏是关闭状态<br/>
     *         如果返回时打开状态状态，会先关闭扩展栏再返回值
     */
    public boolean onBackPressed() {
        if (chatExtendMenuContainer.getVisibility() == View.VISIBLE) {
            hideExtendMenuContainer();
            return false;
        } else {
            return true;
        }
    }

    public void setChatInputMenuListener(ChatInputMenuListener listener) {
        this.listener = listener;
    }

    public interface ChatInputMenuListener {
        /**
         * 发送消息按钮点击
         * 
         * @param content
         *            文本内容
         */
        void onSendMessage(String content, List<Long> at, List<String> atNames);

        /**
         * 长按说话按钮touch事件
         * @param v
         * @param event
         * @return
         */
        boolean onPressToSpeakBtnTouch(View v, MotionEvent event);

        /**
         * edittext 焦点变化
         * @param hasFocus
         */
        void onFocusChanged(boolean hasFocus);

        /**
         * 用户输入@
         */
        void onAt();

    }
    
}
