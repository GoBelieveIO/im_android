package com.beetle.push.core.log;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * NgdsLogHandler
 * 处理将日志写入文件的工作
 */
public class LogFileHandler {
    private final static String TAG = "LogFileHandler";

    //防止日志丢失,采用两个日志文件轮流进行写入
    private static File mLogFilePartOne;
    private static File mLogFilePartTwo;
    private static File mCurrentLogFile;
    private static FileWriter mFileWriter;

    private static LogFileHandler mNgdsLog;
    private List<String> mLogList;

    private Date mDate;
    private DateFormat dateFormat = new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private Context mContext;
    private String mLogFileName;

    public static LogFileHandler getInstance() {
        if (null == mNgdsLog) {
            synchronized (LogFileHandler.class) {
                if (null == mNgdsLog) {
                    mNgdsLog = new LogFileHandler();
                }
            }
        }
        return mNgdsLog;
    }

    public void init(Context context, String logFileName) {
        mContext = context.getApplicationContext();
        mLogFileName = logFileName;
        initIO();
    }

    private LogFileHandler() {
        mLogList = new ArrayList<String>();
        mDate = new Date();
    }


    public void log2File(String log) {
        String logContent = formatCurrentDate() + " " + log;
        synchronized (this) {
            if (null == mCurrentLogFile || !mCurrentLogFile.isFile()) {
                initIO();
            }
            try {
                mFileWriter.append(logContent + "\n");
                mFileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            checkWhetherRotateFile();
        }
    }


    public List<String> synIOLogs() {
        List<String> toIOLogList;
        synchronized (this) {
            toIOLogList = mLogList;
            mLogList = new ArrayList<String>();
        }
        return toIOLogList;
    }



    //may cause currency exception, only recommend use for test
    public void reInitLog() {
        if (null != mLogFilePartOne) {
            mLogFilePartOne.delete();
        }
        if (null != mLogFilePartTwo) {
            mLogFilePartTwo.delete();
        }
    }

    //only recommend use for test
    public String readLastLine() {
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(mCurrentLogFile, "r");
            long len = rf.length();
            long start = rf.getFilePointer();
            long nextend = start + len - 1;
            String line;
            rf.seek(nextend);
            int c = -1;
            while (nextend > start) {
                c = rf.read();
                if (c == '\n' || c == '\r') {
                    line = rf.readLine();
                    if (line != null) {
                        return line;
                    }
                    nextend--;
                }
                nextend--;
                rf.seek(nextend);
                if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行
                    return rf.readLine();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void checkWhetherRotateFile() {
        if (null != mCurrentLogFile) {
            long size = mCurrentLogFile.length();
            if (size > LogConsts.ALL_LOG_MAXSIZE) {
                try {
                    mFileWriter.flush();
                    mFileWriter.close();
                    mFileWriter = null;
                    if (mCurrentLogFile == mLogFilePartOne) {
                        mCurrentLogFile = mLogFilePartTwo;
                    } else {
                        mCurrentLogFile = mLogFilePartOne;
                    }
                    mCurrentLogFile.delete();
                    mFileWriter = new FileWriter(mCurrentLogFile, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initIO() {
        File sdcardDir = Environment.getExternalStorageDirectory();
        File storageDir;
        if (null != sdcardDir && sdcardDir.isDirectory()
            && getAvailableExternalMemorySize() > LogConsts.FILE_LIMITSIZE) {
            storageDir =
                new File(
                    sdcardDir.getAbsolutePath() + File.separator + ".ngdslog" + File.separator
                        + mContext.getPackageName());
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
        } else {
            Log.d(TAG, "storageFile is null or space not enough");
            storageDir = new File(String.valueOf(mContext.getFilesDir()));
        }
        mLogFilePartOne = new File(storageDir, mLogFileName + LogConsts.LOG_PART_ONE);
        mLogFilePartTwo = new File(storageDir, mLogFileName + LogConsts.LOG_PART_TWO);
        mCurrentLogFile = chooseAvailableFile();
        initFileWriter();
    }

    /**
     * 获取还剩下多少能可用的存储空间
     *
     * @return
     */
    private static long getAvailableExternalMemorySize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    private static void initFileWriter() {
        closeWriter();
        openWriter();
    }

    private static void openWriter() {
        try {
            mFileWriter = new FileWriter(mCurrentLogFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void closeWriter() {
        try {
            if (mFileWriter != null) {
                mFileWriter.close();
                mFileWriter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File chooseAvailableFile() {
        if (!mLogFilePartOne.exists() && !mLogFilePartTwo.exists()) {
            return mLogFilePartOne;
        }
        if (mLogFilePartOne.exists() && mLogFilePartOne.length() < LogConsts.LOG_MAXSIZE) {
            return mLogFilePartOne;
        }
        return mLogFilePartTwo;
    }

    private String formatCurrentDate() {
        mDate.setTime(System.currentTimeMillis());
        return dateFormat.format(mDate);
    }

}
