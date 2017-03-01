package com.beetle.bauhinia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.gallery.GalleryImage;
import com.beetle.bauhinia.gallery.ui.GalleryUI;
import com.beetle.bauhinia.tools.AudioRecorder;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.DeviceUtil;
import com.beetle.im.*;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.activity.PhotoActivity;


import com.beetle.bauhinia.tools.AudioDownloader;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.easemob.easeui.widget.EaseChatExtendMenu;
import com.easemob.easeui.widget.EaseChatInputMenu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.tools.AudioRecorder;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.DeviceUtil;
import com.beetle.im.*;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.activity.PhotoActivity;
import com.beetle.bauhinia.ChatItemQuickAction.ChatQuickAction;
import static com.beetle.bauhinia.constant.RequestCodes.*;
import com.beetle.imkit.R;


public class MessageActivity extends BaseActivity implements
        SwipeRefreshLayout.OnRefreshListener {

    protected final String TAG = "imservice";

    private static final int PERMISSIONS_REQUEST = 2;


    private static final int IN_MSG = 0;
    private static final int OUT_MSG = 1;

    protected boolean isShowUserName = false;
    private File captureFile;

    protected ArrayList<IMessage> messages = new ArrayList<IMessage>();
    protected HashMap<Integer, IMessage.Attachment> attachments = new HashMap<Integer, IMessage.Attachment>();

    BaseAdapter adapter;

    IMessage playingMessage;

    //录音相关
    protected Handler mHandler = new Handler();
    protected java.util.Timer sixtySecondsTimer;
    protected java.util.Timer recordingTimer;
    protected AlertDialog alertDialog;

    protected ImageView recordingImageBG;

    protected ImageView recordingImage;

    protected TextView recordingText;

    protected Date mBegin;

    protected String recordFileName;

    AudioRecorder audioRecorder;
    AudioUtil audioUtil;

    ListView listview;
    
    static final int ITEM_TAKE_PICTURE = 1;
    static final int ITEM_PICTURE = 2;
    static final int ITEM_LOCATION = 3;

    protected int[] itemStrings = { R.string.attach_take_pic, R.string.attach_picture, R.string.attach_location };
    protected int[] itemdrawables = { R.drawable.ease_chat_takepic_selector, R.drawable.ease_chat_image_selector,
            R.drawable.ease_chat_location_selector };
    protected int[] itemIds = { ITEM_TAKE_PICTURE, ITEM_PICTURE, ITEM_LOCATION };


    protected MyItemClickListener extendMenuItemClickListener;
    protected EaseChatInputMenu inputMenu;

    /**
     * 扩展菜单栏item点击事件
     *
     */
    class MyItemClickListener implements EaseChatExtendMenu.EaseChatExtendMenuItemClickListener{
        @Override
        public void onClick(int itemId, View view) {
            switch (itemId) {
                case ITEM_TAKE_PICTURE: // 拍照
                    takePicture();
                    break;
                case ITEM_PICTURE:
                    getPicture(); // 图库选择图片
                    break;
                case ITEM_LOCATION: // 位置
                    startActivityForResult(new Intent(MessageActivity.this, LocationPickerActivity.class), PICK_LOCATION);
                    break;
                default:
                    break;
            }
        }
    }


    public static class User {
        public long uid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }

    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("%d", uid);
        return u;
    }

    public interface GetUserCallback {
        void onUser(User u);
    }

    protected void asyncGetUser(long uid, GetUserCallback cb) {

    }

    //加载消息发送者的名称和头像信息
    protected void loadUserName(IMessage msg) {
        User u = getUser(msg.sender);

        msg.setSenderAvatar(u.avatarURL);
        if (TextUtils.isEmpty(u.name)) {
            msg.setSenderName(u.identifier);
            final IMessage fmsg = msg;
            asyncGetUser(msg.sender, new GetUserCallback() {
                @Override
                public void onUser(User u) {
                    fmsg.setSenderName(u.name);
                    fmsg.setSenderAvatar(u.avatarURL);
                }
            });
        } else {
            msg.setSenderName(u.name);
        }
    }

    protected void loadUserName(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            loadUserName(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        captureFile = new File(getExternalFilesDir(null), "pic.jpg");

        File f = new File(getCacheDir(), "bh_audio.amr");
        recordFileName = f.getAbsolutePath();
        Log.i(TAG, "record file name:" + recordFileName);

        listview = (ListView)findViewById(R.id.list_view);

        extendMenuItemClickListener = new MyItemClickListener();
        inputMenu = (EaseChatInputMenu)findViewById(R.id.input_menu);
        registerExtendMenuItem();
        // init input menu
        inputMenu.init();
        inputMenu.setChatInputMenuListener(new EaseChatInputMenu.ChatInputMenuListener() {

            @Override
            public void onSendMessage(String content) {
                // 发送文本消息
                sendTextMessage(content);
            }

            @Override
            public boolean onPressToSpeakBtnTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        try {
                            v.setPressed(true);
                            MessageActivity.this.startRecord();
                        } catch (Exception e) {
                            v.setPressed(false);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (event.getY() < 0) {
                             MessageActivity.this.showReleaseToCancelHint();
                        } else {
                             MessageActivity.this.showMoveUpToCancelHint();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (event.getY() < 0) {
                            // discard the recorded audio.
                            MessageActivity.this.discardRecord();
                        } else {
                            // stop recording and send voice file
                            MessageActivity.this.stopRecord();
                        }
                        return true;
                    default:
                        MessageActivity.this.discardRecord();
                        return false;
                }
            }
        });

        SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);

        adapter = new ChatAdapter();
        listview.setAdapter(adapter);

        listview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //hide keyboard
                if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (getCurrentFocus() != null) {
                        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
                inputMenu.hideExtendMenuContainer();
                return false;
            }
        });


        setSubtitle();

        audioUtil = new AudioUtil(this);
        audioUtil.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (MessageActivity.this.playingMessage != null) {
                    MessageActivity.this.playingMessage.setPlaying(false);
                    MessageActivity.this.playingMessage = null;
                }
            }
        });
        audioUtil.setOnStopListener(new AudioUtil.OnStopListener() {
            @Override
            public void onStop(int reason) {
                adapter.notifyDataSetChanged();
            }
        });

        audioRecorder = new AudioRecorder(this, this.recordFileName);

        if (IMService.getInstance().getConnectState() != IMService.ConnectState.STATE_CONNECTED) {
            disableSend();
        }

        requestPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            Log.i(TAG, "granted permission:" + grantResults);
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int recordPermission = (checkSelfPermission(Manifest.permission.RECORD_AUDIO));
            int readExternalPermission = (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
            int fineLocationPermission = (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
            int coarseLocationPermission = (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION));

            ArrayList<String> permissions = new ArrayList<String>();

            if (recordPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (readExternalPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (permissions.size() > 0) {
                String[] array = new String[permissions.size()];
                permissions.toArray(array);
                this.requestPermissions(array, PERMISSIONS_REQUEST);
            }
        }
    }

    /**
     * 注册底部菜单扩展栏item; 覆盖此方法时如果不覆盖已有item，item的id需大于3
     */
    protected void registerExtendMenuItem(){
        for(int i = 0; i < itemStrings.length; i++){
            inputMenu.registerExtendMenuItem(itemStrings[i], itemdrawables[i], itemIds[i], extendMenuItemClickListener);
        }
    }

    protected void loadEarlierData() {}

    static interface ContentTypes {
        public static int UNKNOWN = 0;
        public static int AUDIO = 2;
        public static int IMAGE = 4;
        public static int LOCATION = 6;
        public static int TEXT = 8;
        public static int NOTIFICATION = 10;
        public static int LINK = 12;
    }

    class ChatAdapter extends BaseAdapter implements ContentTypes {
        @Override
        public int getCount() {
            return messages.size();
        }
        @Override
        public Object getItem(int position) {
            return messages.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            final int basic;
            if (messages.get(position).isOutgoing) {
                basic = OUT_MSG;
            } else {
                basic = IN_MSG;
            }
            return getMediaType(position) + basic;
        }

        int getMediaType(int position) {
            IMessage msg = messages.get(position);
            final int media;
            if (msg.content instanceof IMessage.Text) {
                media = TEXT;
            } else if (msg.content instanceof IMessage.Image) {
                media = IMAGE;
            } else if (msg.content instanceof IMessage.Audio) {
                media = AUDIO;
            } else if (msg.content instanceof IMessage.Location) {
                media = LOCATION;
            } else if (msg.content instanceof IMessage.GroupNotification ||
                    msg.content instanceof IMessage.TimeBase ||
                    msg.content instanceof IMessage.Headline) {
                media = NOTIFICATION;
            } else if (msg.content instanceof IMessage.Link) {
                media = LINK;
            } else {
                media = UNKNOWN;
            }

            return media;
        }

        @Override
        public int getViewTypeCount() {
            return 14;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            IMessage msg = messages.get(position);
            MessageRowView rowView = (MessageRowView)convertView;
            if (rowView == null) {
                IMessage.MessageType msgType = msg.content.getType();
                switch (msgType) {
                    case MESSAGE_IMAGE:
                        rowView = new MessageImageView(MessageActivity.this, !msg.isOutgoing, isShowUserName);
                        break;
                    case MESSAGE_AUDIO:
                        rowView = new MessageAudioView(MessageActivity.this, !msg.isOutgoing, isShowUserName);
                        break;
                    case MESSAGE_TEXT:
                        rowView = new MessageTextView(MessageActivity.this, !msg.isOutgoing, isShowUserName);
                        break;
                    case MESSAGE_LOCATION:
                        rowView = new MessageLocationView(MessageActivity.this, !msg.isOutgoing, isShowUserName);
                        break;
                    case MESSAGE_LINK:
                        rowView = new MessageLinkView(MessageActivity.this, !msg.isOutgoing, isShowUserName);
                        break;
                    case MESSAGE_GROUP_NOTIFICATION:
                    case MESSAGE_TIME_BASE:
                    case MESSAGE_HEADLINE:
                        rowView = new MessageNotificationView(MessageActivity.this);
                        break;
                    default:
                        rowView = new MessageTextView(MessageActivity.this, !msg.isOutgoing, isShowUserName);
                        break;
                }

                if (rowView != null) {
                    View contentView = rowView.getContentView();
                    contentView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            IMessage im = (IMessage)v.getTag();
                            Log.i(TAG, "im:" + im.msgLocalID);
                            MessageActivity.this.onMessageClicked(im);
                        }
                    });
                    contentView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            final IMessage im = (IMessage)v.getTag();

                            ArrayList<ChatQuickAction> actions = new ArrayList<ChatQuickAction>();

                            if (im.isFailure()) {
                                actions.add(ChatQuickAction.RESEND);
                            }

                            if (im.content.getType() == IMessage.MessageType.MESSAGE_TEXT) {
                                actions.add(ChatItemQuickAction.ChatQuickAction.COPY);
                            }

                            if (actions.size() == 0) {
                                return true;
                            }

                            ChatItemQuickAction.showAction(MessageActivity.this,
                                    actions.toArray(new ChatQuickAction[actions.size()]),
                                    new ChatItemQuickAction.ChatQuickActionResult() {

                                        @Override
                                        public void onSelect(ChatQuickAction action) {
                                            switch (action) {
                                                case COPY:
                                                    ClipboardManager clipboard =
                                                            (ClipboardManager)MessageActivity.this
                                                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                                                    clipboard.setText(((IMessage.Text) im.content).text);
                                                    break;
                                                case RESEND:
                                                    MessageActivity.this.resend(im);
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    }
                            );
                            return true;
                        }
                    });
                }
            }
            rowView.setMessage(msg);
            return rowView;
        }
    }

    protected void disableSend() {
        inputMenu.disableSend();
    }

    protected void enableSend() {
        inputMenu.enableSend();
    }


    private class VolumeTimerTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MessageActivity.this.refreshVolume();
                }
            });
        }
    }


    private void showReleaseToCancelHint() {
        recordingText.setText(getString(R.string.release_to_cancel));
        recordingText.setBackgroundResource(R.drawable.ease_recording_text_hint_bg);
    }

    private void showMoveUpToCancelHint() {
        recordingText.setText(getString(R.string.move_up_to_cancel));
        recordingText.setBackgroundColor(Color.TRANSPARENT);
    }

    private void showRecordDialog() {
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(
                R.layout.conversation_recording_dialog,
                (ViewGroup) findViewById(R.id.conversation_recording));

        recordingImage = (ImageView) layout
                .findViewById(R.id.conversation_recording_range);
        recordingImageBG = (ImageView) layout
                .findViewById(R.id.conversation_recording_white);

        recordingText = (TextView) layout
                .findViewById(R.id.conversation_recording_text);

        showMoveUpToCancelHint();

        builder = new AlertDialog.Builder(this);
        alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setContentView(layout);
    }

    private void refreshVolume() {
        if (!this.audioRecorder.isRecording()) {
            return;
        }

        int max = this.audioRecorder.getMaxAmplitude();

        if (max != 0) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) recordingImage
                    .getLayoutParams();
            float scale = max / 7000.0f;
            if (scale < 0.3) {
                recordingImage
                        .setImageResource(R.drawable.record_red);
            } else {
                recordingImage
                        .setImageResource(R.drawable.record_green);
            }
            if (scale > 1) {
                scale = 1;
            }
            int height = recordingImageBG.getHeight()
                    - (int) (scale * recordingImageBG.getHeight());
            params.setMargins(0, 0, 0, -1 * height);
            recordingImage.setLayoutParams(params);

            ((View) recordingImage).scrollTo(0, height);
            // Log.i(TAG, "max amplitude: " + max);
            /**
             * 倒计时提醒
             */
            Date now = new Date();
            long between = (mBegin.getTime() + 60000)
                    - now.getTime();
            if (between < 10000) {
                int second = (int) (Math.floor((between / 1000)));
                if (second == 0) {
                    second = 1;
                }
                recordingText.setText("还剩: " + second + "秒");
            }
        }
    }

    private void startRecord() {
        if (DeviceUtil.isFullStorage()) {
            Toast.makeText(this, "您没有可用的SD卡，请退出U盘模式或者插入SD卡", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (audioUtil.isPlaying()) {
            audioUtil.stopPlay();
        }

        mBegin = new Date();
        //删除上次录音生成的文件内容
        new File(recordFileName).delete();
        audioRecorder.startRecord();
        sixtySecondsTimer = new java.util.Timer();
        sixtySecondsTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "recording end by timeout");
                        MessageActivity.this.stopRecord();
                    }
                });
            }
        }, 60000);

        recordingTimer = new java.util.Timer();
        recordingTimer.schedule(new VolumeTimerTask(), 0, 100);

        showRecordDialog();
    }

    private void discardRecord() {
        // stop sixty seconds limit
        if (sixtySecondsTimer != null) {
            sixtySecondsTimer.cancel();
            sixtySecondsTimer = null;
        }
        // stop volume task
        if (recordingTimer != null) {
            recordingTimer.cancel();
            recordingTimer = null;
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        if (MessageActivity.this.audioRecorder.isRecording()) {
            MessageActivity.this.audioRecorder.stopRecord();
        }
    }

    private void stopRecord() {
        // stop sixty seconds limit
        if (sixtySecondsTimer != null) {
            sixtySecondsTimer.cancel();
            sixtySecondsTimer = null;
        }
        // stop volume task
        if (recordingTimer != null) {
            recordingTimer.cancel();
            recordingTimer = null;
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        if (MessageActivity.this.audioRecorder.isRecording()) {
            MessageActivity.this.audioRecorder.stopRecord();
            MessageActivity.this.sendAudioMessage();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            clearConversation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRefresh() {
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeLayout.setRefreshing(false);

        loadEarlierData();
    }

    protected void setSubtitle() {
        IMService.ConnectState state = IMService.getInstance().getConnectState();
        if (state == IMService.ConnectState.STATE_CONNECTING) {
            setSubtitle("连线中");
        } else if (state == IMService.ConnectState.STATE_CONNECTFAIL ||
                state == IMService.ConnectState.STATE_UNCONNECTED) {
            setSubtitle("未连接");
        } else {
            setSubtitle("");
        }
    }

    protected void setSubtitle(String subtitle) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "imactivity destory");
        audioUtil.release();
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    protected void markMessageListened(IMessage imsg) {
        Log.i(TAG, "not implemented");
    }

    protected void resend(IMessage msg) {
        Log.i(TAG, "not implemented");
    }

    void saveMessageAttachment(IMessage msg, String address) {
        Log.i(TAG, "not implemented");
    }

    protected void sendMessageContent(IMessage.MessageContent content) {
        Log.i(TAG, "not implemented");
    }

    void clearConversation() {
        Log.i(TAG, "clearConversation");
        messages = new ArrayList<IMessage>();
        adapter.notifyDataSetChanged();
    }

    private String formatTimeBase(long ts) {
        String s = "";
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((long)(ts) * 1000);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String weeks[] = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        if (isToday(ts)) {
            s = String.format("%02d:%02d", hour, minute);
        } else if (isYesterday(ts)) {
            s = String.format("昨天 %02d:%02d", hour, minute);
        } else if (isInWeek(ts)) {
            s = String.format("%s %02d:%02d", weeks[dayOfWeek - 1], hour, minute);
        } else if (isInYear(ts)) {
            s = String.format("%02d-%02d %02d:%02d", month+1, dayOfMonth, hour, minute);
        } else {
            s = String.format("%d-%02d-%02d %02d:%02d", year, month+1, dayOfMonth, hour, minute);
        }
        return s;
    }

    private boolean isToday(long ts) {
        int now = now();
        return isSameDay(now, ts);
    }

    private boolean isYesterday(long ts) {
        int now = now();
        int yesterday = now - 24*60*60;
        return isSameDay(ts, yesterday);
    }

    private boolean isInWeek(long ts) {
        int now = now();
        //6天前
        long day6 = now - 6*24*60*60;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(day6 * 1000);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        int zero = (int)(cal.getTimeInMillis()/1000);
        return (ts >= zero);
    }

    private boolean isInYear(long ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts*1000);
        int year = cal.get(Calendar.YEAR);

        cal.setTime(new Date());
        int y = cal.get(Calendar.YEAR);

        return (year == y);
    }

    private boolean isSameDay(long ts1, long ts2) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts1 * 1000);
        int year1 = cal.get(Calendar.YEAR);
        int month1 = cal.get(Calendar.MONTH);
        int day1 = cal.get(Calendar.DAY_OF_MONTH);


        cal.setTimeInMillis(ts2 * 1000);
        int year2 = cal.get(Calendar.YEAR);
        int month2 = cal.get(Calendar.MONTH);
        int day2 = cal.get(Calendar.DAY_OF_MONTH);

        return ((year1==year2) && (month1==month2) && (day1==day2));
    }

    protected void resetMessageTimeBase() {
        ArrayList<IMessage> newMessages = new ArrayList<IMessage>();
        IMessage lastMsg = null;
        for (int i = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (msg.content.getType() == IMessage.MessageType.MESSAGE_TIME_BASE) {
                continue;
            }
            //间隔10分钟，添加时间分割线
            if (lastMsg == null || msg.timestamp - lastMsg.timestamp > 10*60) {
                IMessage.TimeBase timeBase = IMessage.newTimeBase(msg.timestamp);
                String s = formatTimeBase(timeBase.timestamp);
                timeBase.description = s;
                IMessage t = new IMessage();
                t.content = timeBase;
                t.timestamp = msg.timestamp;
                newMessages.add(t);
            }
            newMessages.add(msg);

            lastMsg = msg;
        }
        messages = newMessages;
    }

    protected void insertMessage(IMessage imsg) {
        IMessage lastMsg = null;
        if (messages.size() > 0) {
            lastMsg = messages.get(messages.size() - 1);
        }
        //间隔10分钟，添加时间分割线
        if (lastMsg == null || imsg.timestamp - lastMsg.timestamp > 10*60) {
            IMessage.TimeBase timeBase = IMessage.newTimeBase(imsg.timestamp);
            String s = formatTimeBase(timeBase.timestamp);
            timeBase.description = s;
            IMessage t = new IMessage();
            t.content = timeBase;
            t.timestamp = imsg.timestamp;
            messages.add(t);
        }

        messages.add(imsg);

        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);
    }



    protected void sendTextMessage(String text) {
        if (text.length() == 0) {
            return;
        }

        sendMessageContent(IMessage.newText(text));
    }

    protected void sendImageMessage(Bitmap bmp) {
        double w = bmp.getWidth();
        double h = bmp.getHeight();
        double newHeight = 640.0;
        double newWidth = newHeight*w/h;


        Bitmap bigBMP = Bitmap.createScaledBitmap(bmp, (int)newWidth, (int)newHeight, true);

        double sw = 256.0;
        double sh = 256.0*h/w;

        Bitmap thumbnail = Bitmap.createScaledBitmap(bmp, (int)sw, (int)sh, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bigBMP.compress(Bitmap.CompressFormat.JPEG, 100, os);
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os2);

        String originURL = localImageURL();
        String thumbURL = localImageURL();
        try {
            FileCache.getInstance().storeByteArray(originURL, os);
            FileCache.getInstance().storeByteArray(thumbURL, os2);

            String path = FileCache.getInstance().getCachedFilePath(originURL);
            String thumbPath = FileCache.getInstance().getCachedFilePath(thumbURL);

            String tpath = path + "@256w_256h_0c";
            File f = new File(thumbPath);
            File t = new File(tpath);
            f.renameTo(t);

            sendMessageContent(IMessage.newImage("file:" + path, (int)newWidth, (int)newHeight));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected void sendAudioMessage() {
        String tfile = audioRecorder.getPathName();

        try {
            long mduration = AudioUtil.getAudioDuration(tfile);

            if (mduration < 1000) {
                Toast.makeText(this, "录音时间太短了", Toast.LENGTH_SHORT).show();
                return;
            }
            long duration = mduration/1000;

            String url = localAudioURL();

            IMessage.Audio audio = IMessage.newAudio(url, duration);
            FileInputStream is = new FileInputStream(new File(tfile));
            Log.i(TAG, "store audio url:" + audio.url);
            FileCache.getInstance().storeFile(audio.url, is);

            sendMessageContent(audio);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    protected void sendLocationMessage(float longitude, float latitude, String address) {
        IMessage.Location loc = IMessage.newLocation(latitude, longitude);
        loc.address = address;
        sendMessageContent(loc);
    }

    protected String localImageURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/images/"+ uuid.toString() + ".png";
    }

    protected String localAudioURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/audios/" + uuid.toString() + ".amr";
    }



    void getPicture() {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent
                    , getResources().getString(R.string.product_fotos_get_from))
                    , SELECT_PICTURE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, SELECT_PICTURE_KITKAT);
        }
    }

    void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(captureFile));
        startActivityForResult(takePictureIntent, TAKE_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.i(TAG, "take or select picture fail:" + resultCode);
            return;
        }

        if (requestCode == TAKE_PICTURE) {
            if (captureFile.exists()) {
                Log.i(TAG, "take picture success:" + captureFile.getAbsolutePath());
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bmp = BitmapFactory.decodeFile(captureFile.getAbsolutePath(), options);
                sendImageMessage(bmp);
            }
        } else if (requestCode == SELECT_PICTURE || requestCode == SELECT_PICTURE_KITKAT) {
            try {
                Uri selectedImageUri = data.getData();
                Log.i(TAG, "selected image uri:" + selectedImageUri);
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
                sendImageMessage(bmp);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (requestCode == PICK_LOCATION) {
            float longitude = data.getFloatExtra("longitude", 0);
            float latitude = data.getFloatExtra("latitude", 0);
            String address = data.getStringExtra("address");

            Log.i(TAG, "address:" + address + " longitude:" + longitude + " latitude:" + latitude);
            sendLocationMessage(longitude, latitude, address);
        } else {
            Log.i(TAG, "invalide request code:" + requestCode);
            return;
        }
    }

    void play(IMessage message) {
        IMessage.Audio audio = (IMessage.Audio) message.content;
        Log.i(TAG, "url:" + audio.url);
        if (FileCache.getInstance().isCached(audio.url)) {
            try {
                if (audioRecorder.isRecording()) {
                    audioRecorder.stopRecord();
                }
                if (playingMessage != null && playingMessage == message) {
                    //停止播放
                    audioUtil.stopPlay();
                    playingMessage.setPlaying(false);
                    playingMessage = null;
                } else {
                    if (playingMessage != null) {
                        audioUtil.stopPlay();
                        playingMessage.setPlaying(false);
                    }
                    audioUtil.startPlay(FileCache.getInstance().getCachedFilePath(audio.url));
                    playingMessage = message;
                    message.setPlaying(true);
                    if (!message.isListened() && !message.isOutgoing) {
                        message.setListened(true);
                        markMessageListened(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void onMessageClicked(IMessage message) {
        if (message.content instanceof IMessage.Audio) {
            IMessage.Audio audio = (IMessage.Audio) message.content;
            if (FileCache.getInstance().isCached(audio.url)) {
                play(message);
            } else {
                try {
                    AudioDownloader.getInstance().downloadAudio(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (message.content instanceof IMessage.Image) {
            navigateToViewImage(message);
        } else if (message.content.getType() == IMessage.MessageType.MESSAGE_LOCATION) {
            Log.i(TAG, "location message clicked");
            IMessage.Location loc = (IMessage.Location)message.content;
            startActivity(MapActivity.newIntent(this, loc.longitude, loc.latitude));
        } else if (message.content.getType() == IMessage.MessageType.MESSAGE_LINK) {
            IMessage.Link link = (IMessage.Link)message.content;
            Intent intent = new Intent();
            intent.putExtra("url", link.url);
            intent.setClass(this, WebActivity.class);
            startActivity(intent);
        }
    }

    private void navigateToViewImage(IMessage imageMessage) {
        ArrayList<IMessage> imageMessages = getImageMessages();
        if (imageMessages == null) {
            return;
        }

        int position = 0;
        ArrayList<GalleryImage> galleryImages = new ArrayList<GalleryImage>();
        for (IMessage msg : imageMessages) {
            IMessage.Image image = (IMessage.Image) msg.content;
            if (msg.msgLocalID == imageMessage.msgLocalID) {
                position = galleryImages.size();
            }
            galleryImages.add(new GalleryImage(image.url));
        }
        Intent intent = GalleryUI.getCallingIntent(this, galleryImages, position);
        startActivity(intent);
    }

    protected MessageIterator getMessageIterator() {
        return null;
    }

    private ArrayList<IMessage> getImageMessages() {
        ArrayList<IMessage> images = new ArrayList<IMessage>();

        MessageIterator iter = getMessageIterator();
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
                if (msg.content instanceof IMessage.Image) {
                    images.add(msg);
                }
            }
        }
        Collections.reverse(images);
        return images;
    }

    protected void downloadMessageContent(IMessage msg) {
        if (msg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
            IMessage.Audio audio = (IMessage.Audio) msg.content;
            AudioDownloader downloader = AudioDownloader.getInstance();
            if (!FileCache.getInstance().isCached(audio.url) && !downloader.isDownloading(msg)) {
                try {
                    downloader.downloadAudio(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            msg.setDownloading(downloader.isDownloading(msg));
        } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {

        } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_LOCATION) {
            IMessage.Location loc = (IMessage.Location)msg.content;
            IMessage.Attachment attachment = attachments.get(msg.msgLocalID);
            if (attachment != null) {
                loc.address = attachment.address;
            }

            if (TextUtils.isEmpty(loc.address)) {
                queryLocation(msg);
            }
        }
    }

    protected void queryLocation(final IMessage msg) {
        final IMessage.Location loc = (IMessage.Location)msg.content;

        msg.setGeocoding(true);
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(loc.latitude, loc.longitude), 200, GeocodeSearch.AMAP);

        GeocodeSearch mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                if (i == 0 && regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null
                        && regeocodeResult.getRegeocodeAddress().getFormatAddress() != null) {
                    String address = regeocodeResult.getRegeocodeAddress().getFormatAddress();
                    Log.i(TAG, "address:" + address);
                    loc.address = address;

                    saveMessageAttachment(msg, address);
                } else {
                    // 定位失败;
                }
                msg.setGeocoding(false);
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        });

        mGeocodeSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    protected void downloadMessageContent(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage msg = messages.get(i);
            downloadMessageContent(msg);
        }
    }

    protected IMessage findMessage(int msgLocalID) {
        for (IMessage imsg : messages) {
            if (imsg.msgLocalID == msgLocalID) {
                return imsg;
            }
        }
        return null;
    }

    protected IMessage findMessage(String uuid) {
        for (IMessage imsg : messages) {
            if (imsg.getUUID().equals(uuid)) {
                return imsg;
            }
        }
        return null;
    }
}
