/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.beetle.bauhinia.activity.*;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.*;
import com.beetle.bauhinia.db.message.Notification;
import com.beetle.bauhinia.gallery.GalleryImage;
import com.beetle.bauhinia.gallery.ui.GalleryUI;
import com.beetle.bauhinia.outbox.OutboxObserver;
import com.beetle.bauhinia.toolbar.EaseChatExtendMenu;
import com.beetle.bauhinia.toolbar.EaseChatInputMenu;
import com.beetle.bauhinia.tools.*;
import com.beetle.bauhinia.view.*;
import com.beetle.im.IMService;
import com.beetle.imkit.BuildConfig;

import com.beetle.bauhinia.ChatItemQuickAction.ChatQuickAction;
import com.beetle.imkit.R;


public class MessageActivity extends MessageAudioActivity implements
        SwipeRefreshLayout.OnRefreshListener {

    protected static final String TAG = "imservice";


    //permission request code
    private static final int PERMISSIONS_REQUEST = 2;
    private static final int CAMERA_PERMISSIONS_REQUEST = 3;//拍摄
    private static final int LOCATION_PERMISSIONS_REQUEST = 4;//位置
    private static final int PHOTO_PERMISSIONS_REQUEST = 5;//图片
    private static final int VOICE_PERMISSIONS_REQUEST = 6;//语音


    //activity request code
    public static final int SELECT_PICTURE = 101;
    public static final int SELECT_PICTURE_KITKAT = 102;
    public static final int TAKE_PICTURE = 103;
    public static final int PICK_LOCATION = 104;
    public static final int CAPTURE_CAMERA = 105;
    public static final int SELECT_FILE = 106;

    private static final int IN_MSG = 0;
    private static final int OUT_MSG = 1;

    protected boolean isShowUserName = false;
    protected boolean isShowReply = true;//显示回复信息
    protected boolean isShowReaded = true;//显示未读/已读

    protected BaseAdapter adapter;
    protected ListView listview;
    protected SwipeRefreshLayout swipeRefresh;
    
    static final int ITEM_TAKE_PICTURE = 1;
    static final int ITEM_PICTURE = 2;
    static final int ITEM_LOCATION = 3;
    static final int ITEM_VIDEO_CALL = 4;
    static final int ITEM_FILE = 5;


    protected static final int ITEM_TAKE_PICTURE_ID = 0;
    protected static final int ITEM_PICTURE_ID = 1;
    protected static final int ITEM_LOCATION_ID = 2;
    protected static final int ITEM_VIDEO_CALL_ID = 3;
    protected static final int ITEM_FILE_ID = 4;

    protected int[] itemStrings = { R.string.attach_take_pic, R.string.attach_picture,
            R.string.attach_location, R.string.attach_video_call, R.string.attach_file };
    protected int[] itemdrawables = { R.drawable.ease_chat_takepic_selector, R.drawable.ease_chat_image_selector,
            R.drawable.ease_chat_location_selector,  R.drawable.ease_chat_video_call_selector, R.drawable.ease_chat_file_selector};
    protected int[] itemIds = { ITEM_TAKE_PICTURE, ITEM_PICTURE, ITEM_LOCATION, ITEM_VIDEO_CALL, ITEM_FILE };

    protected boolean[] items = {true, true, true, true, true};


    protected MenuItemClickListener extendMenuItemClickListener;
    protected EaseChatInputMenu inputMenu;

    /**
     * 扩展菜单栏item点击事件
     *
     */
    class MenuItemClickListener implements EaseChatExtendMenu.EaseChatExtendMenuItemClickListener{
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
                    if (checkLocationPermission()) {
                        startActivityForResult(new Intent(MessageActivity.this, LocationPickerActivity.class), PICK_LOCATION);
                    } else {
                        requestLocationPermission();
                    }
                    break;
                case ITEM_VIDEO_CALL:
                    call();
                    break;
                case ITEM_FILE:
                    getFile();
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

        extendMenuItemClickListener = new MenuItemClickListener();
        inputMenu = (EaseChatInputMenu)findViewById(R.id.input_menu);
        registerExtendMenuItem();
        inputMenu.init();
        inputMenu.setChatInputMenuListener(new EaseChatInputMenu.ChatInputMenuListener() {

            @Override
            public void onSendMessage(String content, List<Long> at, List<String> atNames) {
                // 发送文本消息
                sendTextMessage(content, at, atNames);
            }

            @Override
            public void onFocusChanged(boolean hasFocus) {
                if (hasFocus) {
                    onInputFocusChanged(hasFocus);
                }
            }

            @Override
            public boolean onPressToSpeakBtnTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        try {
                            v.setPressed(true);
                            if (!checkRecordPermission()) {
                                //用户需要再次操作
                                requestRecordPermission();
                            } else {
                                MessageActivity.this.startRecord();
                            }
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
            @Override
            public void onAt() {
                MessageActivity.this.onAt();
            }
        });

        SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeRefresh = swipeLayout;

        listview = (ListView)findViewById(R.id.list_view);
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
                inputMenu.clearFocus();
                return false;
            }
        });

        listview.setOnScrollListener(new AbsListView.OnScrollListener() {
            private boolean reachBottom = false;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE && reachBottom) {
                    reachBottom = false;
                    int count = loadLaterData();
                    if (count > 0) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int visibleThreshold = 2;
                if (totalItemCount <= (firstVisibleItem + visibleItemCount + visibleThreshold)) {
                    reachBottom = true;
                }
            }
        });

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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "on back pressed");
        //hide keyboard
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
        inputMenu.hideExtendMenuContainer();
        inputMenu.clearFocus();

        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "imactivity destory");
        audioUtil.release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        boolean granted = true;
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }

        if (!granted) {
            return;
        }

        if (requestCode == CAMERA_PERMISSIONS_REQUEST) {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("dir", getCacheDir().getAbsolutePath());
            startActivityForResult(intent, CAPTURE_CAMERA);
        } else if (requestCode == LOCATION_PERMISSIONS_REQUEST) {
            startActivityForResult(new Intent(MessageActivity.this, LocationPickerActivity.class), PICK_LOCATION);
        } else if (requestCode == PHOTO_PERMISSIONS_REQUEST) {
            getPicture();
        }
    }

    private boolean checkRecordPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int recordPermission = (checkSelfPermission(Manifest.permission.RECORD_AUDIO));
            return recordPermission == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int readExternalPermission = (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
            return readExternalPermission == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int fineLocationPermission = (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
            int coarseLocationPermission = (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
            return fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private boolean checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int recordPermission = (checkSelfPermission(Manifest.permission.RECORD_AUDIO));
            int cameraPermission = (checkSelfPermission(Manifest.permission.CAMERA));
            return recordPermission == PackageManager.PERMISSION_GRANTED && cameraPermission == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestRecordPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] array = {Manifest.permission.RECORD_AUDIO};
            this.requestPermissions(array, VOICE_PERMISSIONS_REQUEST);
        }
    }


    private void requestPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] array = {Manifest.permission.READ_EXTERNAL_STORAGE};
            this.requestPermissions(array, PHOTO_PERMISSIONS_REQUEST);
        }
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] array = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            this.requestPermissions(array, LOCATION_PERMISSIONS_REQUEST);
        }
    }


    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] array = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
            this.requestPermissions(array, CAMERA_PERMISSIONS_REQUEST);
        }
    }

    /**
     * 注册底部菜单扩展栏item; 覆盖此方法时如果不覆盖已有item，item的id需大于3
     */
    protected void registerExtendMenuItem(){
        for(int i = 0; i < itemStrings.length; i++){
            if (items[i]) {
                inputMenu.registerExtendMenuItem(itemStrings[i], itemdrawables[i], itemIds[i], extendMenuItemClickListener);
            }
        }
    }

    static interface ContentTypes {
        public static int UNKNOWN = 0;
        public static int AUDIO = 2;
        public static int IMAGE = 4;
        public static int LOCATION = 6;
        public static int TEXT = 8;
        public static int NOTIFICATION = 10;
        public static int LINK = 12;
        public static int VOIP = 14;
        public static int FILE = 16;
        public static int VIDEO = 18;
        public static int CLASSROOM = 20;

    }
    private static int VIEW_TYPE_COUNT = 22;

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
            if (msg.content instanceof Text) {
                media = TEXT;
            } else if (msg.content instanceof Image) {
                media = IMAGE;
            } else if (msg.content instanceof Audio) {
                media = AUDIO;
            } else if (msg.content instanceof Location) {
                media = LOCATION;
            } else if (msg.content instanceof Notification) {
                media = NOTIFICATION;
            } else if (msg.content instanceof Link) {
                media = LINK;
            } else if (msg.content instanceof VOIP) {
                media = VOIP;
            } else if (msg.content instanceof com.beetle.bauhinia.db.message.File) {
                media = FILE;
            } else if (msg.content instanceof Video) {
                media = VIDEO;
            } else if (msg.content instanceof Classroom) {
                media = CLASSROOM;
            } else {
                media = UNKNOWN;
            }

            return media;
        }

        @Override
        public int getViewTypeCount() {
            return VIEW_TYPE_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            IMessage msg = messages.get(position);
            MessageRowView rowView = (MessageRowView)convertView;
            if (rowView == null) {
                MessageContent.MessageType msgType = msg.content.getType();
                if (msg.content instanceof Notification) {
                    rowView = new MiddleMessageView(MessageActivity.this, msgType);
                } else  if (msg.isOutgoing) {
                    OutMessageView msgView = new OutMessageView(MessageActivity.this, msgType, isShowReply, isShowReaded);

                    msgView.readedButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            IMessage im = (IMessage)v.getTag();
                            Log.i(TAG, "im:" + im.msgLocalID);
                            MessageActivity.this.openUnread(im);
                        }
                    });
                    rowView = msgView;

                } else {
                    rowView = new InMessageView(MessageActivity.this, msgType, isShowUserName, isShowReply);
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
                            ArrayList<ChatQuickAction> actions = getLongClickActions(im);
                            if (actions.size() == 0) {
                                return true;
                            }

                            ChatItemQuickAction.showAction(MessageActivity.this,
                                    actions.toArray(new ChatQuickAction[actions.size()]),
                                    new ChatItemQuickAction.ChatQuickActionResult() {
                                        @Override
                                        public void onSelect(ChatQuickAction action) {
                                            onActionClickListener(action, im);
                                        }
                                    }
                            );
                            return true;
                        }
                    });
                    if (rowView.getContentFrame() != null) {
                        View contentFrame = rowView.getContentFrame();
                        contentFrame.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                final IMessage im = (IMessage)v.getTag();
                                ArrayList<ChatQuickAction> actions = getLongClickActions(im);
                                if (actions.size() == 0) {
                                    return true;
                                }

                                ChatItemQuickAction.showAction(MessageActivity.this,
                                        actions.toArray(new ChatQuickAction[actions.size()]),
                                        new ChatItemQuickAction.ChatQuickActionResult() {
                                            @Override
                                            public void onSelect(ChatQuickAction action) {
                                                onActionClickListener(action, im);
                                            }
                                        }
                                );
                                return true;
                            }
                        });
                    }
                    if (msgType == MessageContent.MessageType.MESSAGE_TEXT) {
                        MessageTextView messageTextView = (MessageTextView)rowView.getContentView();
                        messageTextView.setDoubleTapListener(new MessageTextView.DoubleTapListener() {
                            @Override
                            public void onDoubleTap(MessageTextView v) {
                                Text t = (Text)v.getMessage().content;
                                Log.i(TAG, "double click:" + t.text);

                                Intent intent = new Intent();
                                intent.setClass(MessageActivity.this, OverlayActivity.class);
                                intent.putExtra("text", t.text);
                                MessageActivity.this.startActivity(intent);
                            }
                        });
                    }

                    if (rowView.getReplyButton() != null) {
                        Button replyBtn = rowView.getReplyButton();
                        replyBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                IMessage im = (IMessage)v.getTag();
                                Log.i(TAG, "im:" + im.msgLocalID);
                                MessageActivity.this.openReply(im);
                            }
                        });
                    }
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

    public void onRefresh() {
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeLayout.setRefreshing(false);

        int count = loadEarlierData();
        if (count > 0) {
            adapter.notifyDataSetChanged();
            listview.setSelection(count);
        }
    }



    protected void clearConversation() {
        Log.i(TAG, "clearConversation");
        messages = new ArrayList<IMessage>();
        adapter.notifyDataSetChanged();
    }


    @Override
    protected void insertMessage(IMessage imsg) {
        super.insertMessage(imsg);
        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);
    }

    @Override
    protected void replaceMessage(IMessage imsg, IMessage other) {
        super.replaceMessage(imsg, other);
        adapter.notifyDataSetChanged();
    }

    protected void onInputFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (hasLateMore) {
                hasLateMore = false;
                this.messageID = 0;
                messages = new ArrayList<IMessage>();
                loadData();
                adapter.notifyDataSetChanged();
                //scroll to bottom
                if (messages.size() > 0) {
                    listview.setSelection(messages.size() - 1);
                }
            } else {
                listview.setSelection(messages.size() - 1);
            }
        }
    }

    void getPicture() {
        if (!this.checkPhotoPermission()) {
            requestPhotoPermission();
            return;
        }

        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent
                    , getString(R.string.product_fotos_get_from))
                    , SELECT_PICTURE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, SELECT_PICTURE_KITKAT);
        }
    }

    void takePicture() {
        if (checkCameraPermission()) {
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("dir", getCacheDir().getAbsolutePath());
            startActivityForResult(intent, CAPTURE_CAMERA);
        } else {
            requestCameraPermission();
        }
    }

    void getFile() {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        chooseFile = Intent.createChooser(chooseFile, "选择文件");
        startActivityForResult(chooseFile, SELECT_FILE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            Log.i(TAG, "take or select picture fail:" + resultCode);
            return;
        }

        if (requestCode == SELECT_PICTURE || requestCode == SELECT_PICTURE_KITKAT) {
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
        } else if (requestCode == CAPTURE_CAMERA) {
            String videoPath = data.getStringExtra("video_path");
            String thumbPath = data.getStringExtra("thumbnail_path");
            String picturePath = data.getStringExtra("picture_path");

            if (!TextUtils.isEmpty(picturePath)) {
                Log.i(TAG, "take picture success:" + picturePath);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bmp = BitmapFactory.decodeFile(picturePath, options);
                sendImageMessage(bmp);

                //删除临时文件
                new File(picturePath).delete();
            } else if (!TextUtils.isEmpty(videoPath) && !TextUtils.isEmpty(thumbPath)) {
                Log.i(TAG, "take video success:" + videoPath + " thumbnail path:" + thumbPath);
                sendVideoMessage(videoPath, thumbPath);
            }
        } else if (requestCode == SELECT_FILE) {
            Uri fileUri = data.getData();
            sendFileMessage(fileUri);
        } else {
            Log.i(TAG, "invalide request code:" + requestCode);
            return;
        }
    }


    protected void onMessageClicked(IMessage message) {
        if (message.content instanceof Audio) {
            Audio audio = (Audio) message.content;
            if (FileCache.getInstance().isCached(audio.url)) {
                play(message);
            } else {
                FileDownloader.getInstance().download(message);
            }
        } else if (message.content instanceof Image) {
            navigateToViewImage(message);
        } else if (message.content.getType() == MessageContent.MessageType.MESSAGE_LOCATION) {
            Log.i(TAG, "location message clicked");
            Location loc = (Location)message.content;
            startActivity(MapActivity.newIntent(this, loc.longitude, loc.latitude));
        } else if (message.content.getType() == MessageContent.MessageType.MESSAGE_LINK) {
            Link link = (Link)message.content;
            Intent intent = new Intent();
            intent.putExtra("url", link.url);
            intent.setClass(this, WebActivity.class);
            startActivity(intent);
        } else if (message.getType() == MessageContent.MessageType.MESSAGE_FILE) {
            com.beetle.bauhinia.db.message.File f = (com.beetle.bauhinia.db.message.File)message.content;
            Intent intent = new Intent();
            intent.putExtra("url", f.url);
            intent.putExtra("size", f.size);
            intent.putExtra("filename", f.filename);
            intent.setClass(this, MessageFileActivity.class);
            startActivity(intent);
        } else if (message.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video v = (Video)message.content;
            Intent intent = new Intent();
            intent.putExtra("url", v.url);
            intent.putExtra("sender", message.sender);
            intent.putExtra("secret", message.secret);
            intent.setClass(this, PlayerActivity.class);
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
            Image image = (Image) msg.content;
            if (msg.msgLocalID == imageMessage.msgLocalID) {
                position = galleryImages.size();
            }
            galleryImages.add(new GalleryImage(image.url));
        }
        Intent intent = GalleryUI.getCallingIntent(this, galleryImages, position);
        startActivity(intent);
    }

    protected void call() {}

    protected void onAt() {}

    protected void forward(IMessage im) {}

    protected void openUnread(IMessage im) {}

    protected void openReply(IMessage message) {

    }

    protected ArrayList<ChatQuickAction> getLongClickActions(IMessage im) {
        ArrayList<ChatQuickAction> actions = new ArrayList<ChatQuickAction>();

        if (im.content.getType() == MessageContent.MessageType.MESSAGE_TEXT) {
            actions.add(ChatItemQuickAction.ChatQuickAction.COPY);
        }

        if (im.isFailure()) {
            actions.add(ChatQuickAction.RESEND);
        } else {
            if (im.content.getType() == MessageContent.MessageType.MESSAGE_TEXT ||
                    im.content.getType() == MessageContent.
                            MessageType.MESSAGE_IMAGE ||
                    im.content.getType() == MessageContent.
                            MessageType.MESSAGE_AUDIO ||
                    im.content.getType() == MessageContent.
                            MessageType.MESSAGE_VIDEO ||
                    im.content.getType() == MessageContent.
                            MessageType.MESSAGE_LOCATION ||
                    im.content.getType() == MessageContent.MessageType.MESSAGE_FILE) {
                actions.add(ChatQuickAction.FORWARD);
            }
        }
        int now = now();
        if (now >= im.timestamp && (now - im.timestamp) < (REVOKE_EXPIRE-10) && im.isOutgoing) {
            actions.add(ChatQuickAction.REVOKE);
        }
        return actions;
    }

    protected void onActionClickListener(ChatQuickAction action, IMessage im) {
        switch (action) {
            case COPY:
                ClipboardManager clipboard =
                        (ClipboardManager)MessageActivity.this
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(((Text) im.content).text);
                break;
            case RESEND:
                MessageActivity.this.resend(im);
                break;
            case REVOKE:
                MessageActivity.this.revoke(im);
                break;
            case FORWARD:
                MessageActivity.this.forward(im);
            default:
                break;
        }

    }

}
