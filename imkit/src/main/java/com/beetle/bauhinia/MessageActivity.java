package com.beetle.bauhinia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
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
import com.beetle.bauhinia.tools.Outbox;
import com.easemob.easeui.widget.EaseChatExtendMenu;
import com.easemob.easeui.widget.EaseChatInputMenu;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.beetle.bauhinia.ChatItemQuickAction.ChatQuickAction;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


import com.beetle.imkit.R;

import static com.beetle.bauhinia.constant.RequestCodes.*;


public class MessageActivity extends BaseActivity implements
        AudioDownloader.AudioDownloaderObserver,
        Outbox.OutboxObserver,
        SwipeRefreshLayout.OnRefreshListener {

    protected final String TAG = "imservice";



    private static final int IN_MSG = 0;
    private static final int OUT_MSG = 1;

    protected String sendNotificationName;
    protected String clearNotificationName;


    protected long sender;
    protected long receiver;
    protected boolean isShowUserName = false;

    protected HashMap<Long, String> names = new HashMap<Long, String>();
    protected ArrayList<IMessage> messages = new ArrayList<IMessage>();

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
    TextView titleView;
    TextView subtitleView;
    Toolbar toolbar;

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
                    //startActivityForResult(new Intent(getActivity(), EaseBaiduMapActivity.class), REQUEST_CODE_MAP);
                    break;
                default:
                    break;
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        File f = new File(getCacheDir(), "bh_audio.amr");
        recordFileName = f.getAbsolutePath();
        Log.i(TAG, "record file name:" + recordFileName);

        listview = (ListView)findViewById(R.id.list_view);
        titleView = (TextView)findViewById(R.id.title);
        subtitleView = (TextView)findViewById(R.id.subtitle);
        toolbar = (Toolbar)findViewById(R.id.support_toolbar);

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
        setSupportActionBar(toolbar);

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

        AudioDownloader.getInstance().addObserver(this);

        Outbox.getInstance().addObserver(this);

        if (IMService.getInstance().getConnectState() != IMService.ConnectState.STATE_CONNECTED) {
            disableSend();
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
            if (isOutMsg(position)) {
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
            } else if (msg.content instanceof IMessage.GroupNotification) {
                media = NOTIFICATION;
            } else {
                media = UNKNOWN;
            }

            return media;
        }

        boolean isOutMsg(int position) {
            IMessage msg = messages.get(position);
            return msg.sender == MessageActivity.this.sender;
        }

        @Override
        public int getViewTypeCount() {
            return 12;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            IMessage msg = messages.get(position);
            MessageRowView rowView = (MessageRowView)convertView;
            if (rowView == null) {
                IMessage.MessageType msgType = msg.content.getType();
                switch (msgType) {
                    case MESSAGE_IMAGE:
                        rowView = new MessageImageView(MessageActivity.this, !isOutMsg(position), isShowUserName);
                        break;
                    case MESSAGE_AUDIO:
                        rowView = new MessageAudioView(MessageActivity.this, !isOutMsg(position), isShowUserName);
                        break;
                    case MESSAGE_TEXT:
                        rowView = new MessageTextView(MessageActivity.this, !isOutMsg(position), isShowUserName);
                        break;
                    case MESSAGE_GROUP_NOTIFICATION:
                        rowView = new MessageNotificationView(MessageActivity.this);
                        break;
                    default:
                        rowView = new MessageTextView(MessageActivity.this, !isOutMsg(position), isShowUserName);
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

            rowView.setMessage(msg, !isOutMsg(position));
            return rowView;
        }
    }

    protected void disableSend() {
        inputMenu.disableSend();
    }

    protected void enableSend() {
        inputMenu.enableSend();
    }

    void resend(IMessage msg) {
        eraseMessageFailure(msg);
        msg.setFailure(false);
        this.sendMessage(msg);
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
            messages = new ArrayList<IMessage>();
            adapter.notifyDataSetChanged();

            NotificationCenter nc = NotificationCenter.defaultCenter();
            Notification notification = new Notification(this.receiver, clearNotificationName);
            nc.postNotification(notification);
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
        subtitleView.setText(subtitle);
        if (subtitle.length() > 0) {
            subtitleView.setVisibility(View.VISIBLE);
        } else {
            subtitleView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "imactivity destory");

        AudioDownloader.getInstance().removeObserver(this);
        Outbox.getInstance().removeObserver(this);
        audioUtil.release();
    }


    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    void sendMessage(IMessage imsg) {
        Log.i(TAG, "not implemented");
    }

    void saveMessage(IMessage imsg) {
        Log.i(TAG, "not implemented");
    }

    void markMessageFailure(IMessage imsg) {
        Log.i(TAG, "not implemented");
    }

    void eraseMessageFailure(IMessage imsg) {
        Log.i(TAG, "not implemented");
    }

    void clearConversation() {
        Log.i(TAG, "not implemented");
    }

    void insertMessage(IMessage imsg) {
        messages.add(imsg);

        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);
    }
    void sendTextMessage(String text) {
        if (text.length() == 0) {
            return;
        }

        IMessage imsg = new IMessage();
        imsg.sender = this.sender;
        imsg.receiver = this.receiver;
        imsg.setContent(IMessage.newText(text));
        imsg.timestamp = now();

        saveMessage(imsg);
        sendMessage(imsg);

        insertMessage(imsg);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(imsg, sendNotificationName);
        nc.postNotification(notification);
    }

    void sendImageMessage(Bitmap bmp) {
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

            IMessage imsg = new IMessage();
            imsg.sender = this.sender;
            imsg.receiver = this.receiver;
            imsg.setContent(IMessage.newImage("file:" + path));
            imsg.timestamp = now();
            saveMessage(imsg);

            insertMessage(imsg);

            sendMessage(imsg);

            NotificationCenter nc = NotificationCenter.defaultCenter();
            Notification notification = new Notification(imsg, sendNotificationName);
            nc.postNotification(notification);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String localImageURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/images/"+ uuid.toString() + ".png";
    }

    private String localAudioURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/audios/" + uuid.toString() + ".amr";
    }


    private void sendAudioMessage() {
        String tfile = audioRecorder.getPathName();

        try {
            long mduration = AudioUtil.getAudioDuration(tfile);

            if (mduration < 1000) {
                Toast.makeText(this, "录音时间太短了", Toast.LENGTH_SHORT).show();
                return;
            }
            long duration = mduration/1000;

            String url = localAudioURL();
            IMessage imsg = new IMessage();
            imsg.sender = this.sender;
            imsg.receiver = this.receiver;
            imsg.setContent(IMessage.newAudio(url, duration));
            imsg.timestamp = now();

            saveMessage(imsg);

            Log.i(TAG, "msg local id:" + imsg.msgLocalID);

            insertMessage(imsg);

            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            FileInputStream is = new FileInputStream(new File(tfile));
            Log.i(TAG, "store audio url:" + audio.url);
            FileCache.getInstance().storeFile(audio.url, is);

            sendMessage(imsg);

            NotificationCenter nc = NotificationCenter.defaultCenter();
            Notification notification = new Notification(imsg, sendNotificationName);
            nc.postNotification(notification);

        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
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
        startActivityForResult(takePictureIntent, TAKE_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.i(TAG, "take or select picture fail:" + resultCode);
            return;
        }

        Bitmap bmp;
        if (requestCode == TAKE_PICTURE) {
            bmp = (Bitmap) data.getExtras().get("data");
        } else if (requestCode == SELECT_PICTURE || requestCode == SELECT_PICTURE_KITKAT)  {
            try {
                Uri selectedImageUri = data.getData();
                Log.i(TAG, "selected image uri:" + selectedImageUri);
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bmp = BitmapFactory.decodeStream(is, null, options);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            Log.i(TAG, "invalide request code:" + requestCode);
            return;
        }
        sendImageMessage(bmp);
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
            IMessage.Image image = (IMessage.Image) message.content;
            startActivity(PhotoActivity.newIntent(this, image.image));
        }
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
            msg.setUploading(Outbox.getInstance().isUploading(msg));
        } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            msg.setUploading(Outbox.getInstance().isUploading(msg));
        }
    }

    protected void downloadMessageContent(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage msg = messages.get(i);
            downloadMessageContent(msg);
        }
    }

    @Override
    public void onAudioDownloadSuccess(IMessage msg) {
        Log.i(TAG, "audio download success");
    }
    @Override
    public void onAudioDownloadFail(IMessage msg) {
        Log.i(TAG, "audio download fail");
    }

    @Override
    public void onAudioUploadSuccess(IMessage imsg, String url) {
        Log.i(TAG, "audio upload success:" + url);

    }

    @Override
    public void onAudioUploadFail(IMessage msg) {
        Log.i(TAG, "audio upload fail");
        markMessageFailure(msg);
        msg.setFailure(true);
    }

    @Override
    public void onImageUploadSuccess(IMessage imsg, String url) {
        Log.i(TAG, "image upload success:" + url);
    }

    @Override
    public void onImageUploadFail(IMessage msg) {
        Log.i(TAG, "image upload fail");
        this.markMessageFailure(msg);
        msg.setFailure(true);
    }
}
