/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beetle.push.core.log;

import android.content.Context;


/**
 * Logging helper class.
 */
public class PushLog {


    private static boolean DEBUG = true;

    protected static LogFileHandler sLogHandler;

    /**
     * 初始化日志本地化输出渠道(调用此方法后日志将输出到文件中)
     *
     * @param context
     * @param logFileName 请用模块名称加上版本号作为唯一文件名
     */
    public static void initFileLoger(Context context, String logFileName) {
        sLogHandler = LogFileHandler.getInstance();
        sLogHandler.init(context, logFileName);
    }

    public static void v(String tag, String content) {
        android.util.Log.v(tag, content);
        log2File("v", tag, content);
    }

    public static void d(String tag, String content) {
        android.util.Log.d(tag, content);
        log2File("d", tag, content);
    }

    public static void e(String tag, String content) {
        android.util.Log.e(tag, content);
        log2File("e", tag, content);
    }

    public static void e(String tag, Throwable tr) {
        String content = tr.toString();
        android.util.Log.e(tag, content, tr);
        log2File("e", tag, content);
    }

    public static void e(String tag, Throwable tr, String content) {
        android.util.Log.e(tag, content, tr);
        log2File("e", tag, content);
    }

    private static void log2File(String prefix, String tag, String content) {
        if (DEBUG && null != sLogHandler) {
            sLogHandler.log2File(prefix + " " + tag + ":" + content);
        }
    }
}
