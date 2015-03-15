package com.beetle.im;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 14-7-21.
 */
public abstract class Timer {
    private static final int WHAT = 0;

    private long start;
    private long interval;
    private boolean active = false;

    class TimerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (!active) {
                return;
            }

            Timer.this.fire();
            if (Timer.this.interval != -1) {
                long t = uptimeMillis() + Timer.this.interval;
                boolean b = this.sendEmptyMessageAtTime(WHAT, t);
            }
        }
    }
    private Handler handler = new TimerHandler();

    public void setTimer(long start, long interval) {
        this.start = start;
        this.interval = interval;
        if (active) {
            handler.removeMessages(WHAT);
            handler.sendEmptyMessageAtTime(WHAT, start);
        }
    }

    public void setTimer(long start) {
        this.start = start;
        this.interval = -1;
        if (active) {
            handler.removeMessages(WHAT);
            handler.sendEmptyMessageAtTime(WHAT, start);
        }
    }

    public void resume() {
        active = true;
        handler.sendEmptyMessageAtTime(WHAT, start);
    }

    public void suspend() {
        active = false;
        handler.removeMessages(WHAT);
    }

    protected abstract void fire();
}