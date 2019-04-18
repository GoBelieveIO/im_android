package com.beetle.bauhinia.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Text;
import com.beetle.imkit.R;
import com.easemob.easeui.widget.emoticon.EmoticonManager;


public class MessageTextView extends MessageContentView {
    private TextView textView;
    private GestureDetector mGestureDetector;

    private long time;


    public interface DoubleTapListener {
        void onDoubleTap(MessageTextView v);
    }

    private DoubleTapListener doubleTapListener;
    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            if (doubleTapListener != null) {
                doubleTapListener.onDoubleTap(MessageTextView.this);
            }
            return true;
        }
    }

    public MessageTextView(Context context) {
        super(context);
        inflater.inflate(R.layout.chat_content_text, this);

        mGestureDetector = new GestureDetector(context, new GestureListener());
        textView = (TextView)findViewById(R.id.text);
        textView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    time = System.currentTimeMillis();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - time > 500) {
                        //long click
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void setDoubleTapListener(DoubleTapListener l) {
        this.doubleTapListener = l;
    }

    @Override
    public void setMessage(IMessage msg) {
        super.setMessage(msg);

        this.textView.setTag(message);

        MessageContent.MessageType mediaType = message.content.getType();

        if (mediaType == MessageContent.MessageType.MESSAGE_TEXT) {
            TextView content = (TextView) findViewById(R.id.text);
            String text = ((Text) msg.content).text;
            content.setText(EmoticonManager.getInstance().getEmoticonStr(text));
        }

        if (msg.isOutgoing) {
            textView.setBackgroundResource(R.drawable.chatto_bg);
        } else {
            textView.setBackgroundResource(R.drawable.chatfrom_bg);
        }
        requestLayout();
    }

    public TextView getTextView() {
        return textView;
    }

}