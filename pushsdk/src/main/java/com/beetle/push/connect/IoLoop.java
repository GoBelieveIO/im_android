package com.beetle.push.connect;



import com.beetle.push.core.log.PushLog;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by houxh on 14-8-5.
 */
public class IoLoop extends Thread {

    private static final String TAG = "IoLoop";


    public static interface Handler {
        public void handleEvent(SelectionKey key);
    }


    public static abstract class Timer {
        private long interval;

        public Timer(long interval) {
            this.interval = interval;
        }

        public Timer() {
            this.interval = -1;
        }

        public long getInterval() {
            return interval;
        }

        public abstract void handleTimeout();
    }


    public static interface IoRunnable {
        public void run();
    }


    private static IoLoop defaultLoop = new IoLoop();
    private Selector selector;
    private Timer timer;
    private long deadline;

    private ArrayList<IoRunnable> tasks;

    public IoLoop() {
        tasks = new ArrayList<IoRunnable>();
    }

    public static IoLoop getDefaultLoop() {
        return defaultLoop;
    }

    public void prepare() throws IOException {
        selector = Selector.open();
    }

    public Selector getSelector() {
        return selector;
    }

    public void asyncSend(IoRunnable async) {
        synchronized (this) {
            tasks.add(async);
        }
        selector.wakeup();
    }

    private void runAsync() {
        ArrayList<IoRunnable> tmp;
        synchronized (this) {
            tmp = tasks;
            tasks = new ArrayList<IoRunnable>();
        }
        for (int i = 0; i < tmp.size(); i++) {
            IoRunnable a = tmp.get(i);
            a.run();
        }
    }

    public void setTimeout(long timeout, Timer timer) {
        if (this.timer != null) {
            if (timer == null) {
                PushLog.d(TAG, "cancel timer");
            } else {
                PushLog.d(TAG, "overwrite timer");
            }
        }

        deadline = System.currentTimeMillis() + timeout;
        this.timer = timer;
    }

    private void runTimer() {
        long now = System.currentTimeMillis();
        long timeout = deadline - now;

        if (timeout <= 0) {
            timeout = 0;
            if (timer != null) {
                long old = deadline;
                Timer oldTimer = timer;
                timer.handleTimeout();

                if (old != deadline || oldTimer != timer) {
                    //在handletimeout内部设置了新的timer
                    return;
                }

                if (timer.getInterval() > 0) {
                    deadline = System.currentTimeMillis() + timer.getInterval();
                } else {
                    timer = null;
                }
            }
        }
    }

    private void runOnce() throws IOException {
        runAsync();
        long now = System.currentTimeMillis();

        runTimer();
        long timeout = deadline - now;
        if (timeout < 0) {
            timeout = 0;
        }

        int r = selector.select(timeout);
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> iter = keys.iterator();

        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            Handler handler = (Handler) key.attachment();
            if (handler == null) {
                PushLog.d(TAG, "null handler");
                continue;
            }
            handler.handleEvent(key);
        }
    }

    public void run() {
        while (true) {
            try {
                runOnce();
            } catch (IOException e) {
                PushLog.d(TAG, "io exception:" + e);
                return;
            }
        }
    }
}
