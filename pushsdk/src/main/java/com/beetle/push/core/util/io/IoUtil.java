/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 <<<<<<< HEAD
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 =======
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 >>>>>>> a5806438c9785dfc1466484b7fb450f1b0054417
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.beetle.push.core.util.io;

import java.io.InputStream;
import java.nio.ByteBuffer;


public final class IoUtil {
    private static final char HEX_DIGITS[] = {
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'A',
        'B',
        'C',
        'D',
        'E',
        'F'
    };

    public final static String bin2Hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }


    public static String bin2HexForTest(byte[] deviceToken) {
        StringBuilder sb = new StringBuilder(deviceToken.length * 2);
        for (int i = 0; i < deviceToken.length; i++) {
            sb.append("\\x");
            sb.append(HEX_DIGITS[(deviceToken[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[deviceToken[i] & 0x0f]);
        }
        return sb.toString();
    }

    public final static byte[] hex2bin(String hex) {
        int len = hex.length() >> 1;
        if (len > 0) {
            byte[] data = new byte[len];
            for (int i = 0, hPos = 0; i < len; i++, hPos = i << 1) {
                data[i] = (byte) Integer.parseInt(hex.substring(hPos, hPos + 2), 16);
            }
            return data;
        }
        return null;
    }

    public final static byte[] variableLength(int length) {
        int resLength = 0;
        int result = 0;
        do {
            result |= (length & 0x7F) << 24;
            length >>>= 7;
            resLength++;
            if (length > 0) {
                result >>>= 8;
                result |= 0x80000000;
            }
        }
        while (length > 0);
        byte[] res = new byte[resLength];
        for (int i = 0, move = 24; i < resLength; i++) {
            res[i] = (byte) (result >>> move);
            move -= 8;
        }
        return res;
    }

    public final static int readVariableLength(InputStream is) {
        int length = 0;
        int cur;
        try {
            do {
                cur = is.read();
                length |= (cur & 0x7F);
                if ((cur & 0x80) != 0)
                    length <<= 7;
            }
            while ((cur & 0x80) != 0);
            return length;
        } catch (Exception e) {
            return 0;
        }
    }

    public final static int readVariableLength(ByteBuffer buf) {
        int length = 0;
        int cur;
        if (buf.hasRemaining())
            do {
                cur = buf.get();
                length |= (cur & 0x7F);
                if ((cur & 0x80) != 0)
                    length <<= 7;
            }
            while ((cur & 0x80) != 0 && buf.hasRemaining());
        return length;
    }

    public final static int writeLong(long v, byte[] b, int off) {
        b[off] = (byte) (0xFF & (v >> 56));
        b[off + 1] = (byte) (0xFF & (v >> 48));
        b[off + 2] = (byte) (0xFF & (v >> 40));
        b[off + 3] = (byte) (0xFF & (v >> 32));
        b[off + 4] = (byte) (0xFF & (v >> 24));
        b[off + 5] = (byte) (0xFF & (v >> 16));
        b[off + 6] = (byte) (0xFF & (v >> 8));
        b[off + 7] = (byte) (0xFF & v);

        return 8;
    }

    public final static int writeMac(long v, byte[] b, int off) {
        b[off] = (byte) (0xFF & (v >> 40));
        b[off + 1] = (byte) (0xFF & (v >> 32));
        b[off + 2] = (byte) (0xFF & (v >> 24));
        b[off + 3] = (byte) (0xFF & (v >> 16));
        b[off + 4] = (byte) (0xFF & (v >> 8));
        b[off + 5] = (byte) (0xFF & v);
        return 6;
    }

    public final static int writeInt(int v, byte[] b, int off) {
        b[off] = (byte) (0xFF & (v >> 24));
        b[off + 1] = (byte) (0xFF & (v >> 16));
        b[off + 2] = (byte) (0xFF & (v >> 8));
        b[off + 3] = (byte) (0xFF & v);
        return 4;
    }

    public final static int writeShort(int v, byte[] b, int off) {
        b[off] = (byte) (0xFF & (v >> 8));
        b[off + 1] = (byte) (0xFF & v);
        return 2;
    }

    public final static int writeByte(int v, byte[] b, int off) {
        b[off] = (byte) v;
        return 1;
    }

    public final static int write(byte[] v, int src_off, byte[] b, int off, int len) {
        if (v == null || v.length == 0)
            return 0;
        System.arraycopy(v, src_off, b, off, len);
        return len;
    }

    public final static short readShort(byte[] src, int off) {
        return (short) ((src[off] & 0xFF) << 8 | (src[off + 1] & 0xFF));
    }

    public final static int readUnsignedShort(byte[] src, int off) {
        return ((src[off] & 0xFF) << 8 | (src[off + 1] & 0xFF));
    }

    public final static int readInt(byte[] src, int off) {
        return (src[off] & 0xFF) << 24 | (src[off + 1] & 0xFF) << 16 | (src[off + 2] & 0xFF) << 8
            | (src[off + 3] & 0xFF);
    }

    public final static long readLong(byte[] src, int off) {
        return (src[off] & 0xFFL) << 56 | (src[off + 1] & 0xFFL) << 48
            | (src[off + 2] & 0xFFL) << 40 | (src[off + 3] & 0xFFL) << 32
            | (src[off + 4] & 0xFFL) << 24 | (src[off + 5] & 0xFFL) << 16
            | (src[off + 6] & 0xFFL) << 8 | (src[off + 7] & 0xFFL);
    }

    public final static long readMac(byte[] src, int off) {
        return (src[off] & 0xFFL) << 40 | (src[off + 1] & 0xFFL) << 32
            | (src[off + 2] & 0xFFL) << 24 | (src[off + 3] & 0xFFL) << 16
            | (src[off + 4] & 0xFFL) << 8 | (src[off + 5] & 0xFFL);
    }

    public final static String replace(String from, String to, String source) {
        if (source == null || from == null || to == null)
            return null;
        if (source.indexOf(from) < 0)
            return source;
        StringBuffer bf = new StringBuffer();
        int index = -1;
        while ((index = source.indexOf(from)) != -1) {
            bf.append(source.substring(0, index) + to);
            source = source.substring(index + from.length());
            index = -1;
        }
        bf.append(source);
        return bf.toString();
    }

    public final static String[] splitString(String src, String regular, int limit) {
        return src.split(regular, limit);
    }

    public final static boolean isNumberic(String src) {
        if (src == null || (src = src.trim()).equals(""))
            return false;
        char[] chars = src.toCharArray();
        boolean result = true;
        for (char c : chars) {
            if (c < '0' || c > '9')
                return false;
        }
        return result;
    }



    public final static int PROTOCOL = 0, HOST = 1, PORT = 2, PATH = 3, CONTENT = 4, ARGUMENTS = 5;
    public final static int READ = 1;
    public final static int WRITE = 1 << 1;
    public final static int READ_WRITE = READ | WRITE;

    public final static String[] splitURL(String url) {
        StringBuffer u = new StringBuffer(url.toLowerCase());
        String[] result = new String[6];
        for (int i = 0; i < 6; i++) {
            result[i] = "";
        }
        // get protocol

        int index = url.indexOf(":");
        if (index > 0) {
            result[PROTOCOL] = url.substring(0, index);
            u.delete(0, index + 1);
        } else if (index == 0)
            throw new IllegalArgumentException("url format error - protocol");
        // check for host/port
        if (u.length() >= 2 && u.charAt(0) == '/' && u.charAt(1) == '/') {
            // found domain part
            u.delete(0, 2);
            int slash = u.toString().indexOf('/');
            if (slash < 0) {
                slash = u.length();
            }
            if (slash != 0) {
                int colon = u.toString().indexOf(':');
                int endIndex = slash;
                if (colon >= 0) {
                    if (colon > slash)
                        throw new IllegalArgumentException("url format error - port");
                    endIndex = colon;
                    result[PORT] = u.toString().substring(colon + 1, slash);
                }
                result[HOST] = u.toString().substring(0, endIndex);
                u.delete(0, slash);
            }
        }
        if (u.length() > 0) {
            url = u.toString();
            int slash = url.lastIndexOf('/');
            if (slash > 0)
                result[PATH] = url.substring(0, slash);
            else if (slash == 0) {
                if (url.indexOf('?') > 0)
                    throw new IllegalArgumentException("url format error - path");
                result[PATH] = url;
                return result;
            }
            if (slash < url.length() - 1) {
                String fn = url.substring(slash + 1, url.length());
                int anchorIndex = fn.indexOf('?');
                if (anchorIndex >= 0) {
                    result[CONTENT] = fn.substring(0, anchorIndex);
                    result[ARGUMENTS] = fn.substring(anchorIndex + 1);
                } else {
                    result[CONTENT] = fn;
                }
            }
        } else
            result[PATH] = "/";
        return result;
    }

    public final static String mergeURL(String[] splits) {
        StringBuffer buffer = new StringBuffer();
        if (!splits[PROTOCOL].equals(""))
            buffer.append(splits[PROTOCOL]).append("://");
        if (!splits[HOST].equals(""))
            buffer.append(splits[HOST]);
        if (!splits[PORT].equals(""))
            buffer.append(':').append(splits[PORT]);
        if (!splits[PATH].equals("")) {
            buffer.append(splits[PATH]);
            if (!splits[PATH].equals("/"))
                buffer.append('/');
        }
        if (!splits[CONTENT].equals(""))
            buffer.append(splits[CONTENT]);
        if (!splits[ARGUMENTS].equals(""))
            buffer.append('?').append(splits[ARGUMENTS]);
        return buffer.toString();
    }

    public final static String mergeProxyTarURL(String[] splits) {
        StringBuffer buffer = new StringBuffer();
        if (!splits[HOST].equals(""))
            buffer.append(splits[HOST]);
        if (!splits[PORT].equals(""))
            buffer.append(':').append(splits[PORT]);
        if (!splits[PATH].equals("")) {
            buffer.append(splits[PATH]);
            if (!splits[PATH].equals("/"))
                buffer.append('/');
        }
        if (!splits[CONTENT].equals(""))
            buffer.append(splits[CONTENT]);
        if (!splits[ARGUMENTS].equals(""))
            buffer.append('?').append(splits[ARGUMENTS]);
        return buffer.toString();
    }

    public final static String mergeX_SerLet(String[] splits) {
        StringBuffer buffer = new StringBuffer();
        if (!splits[PATH].equals("")) {
            buffer.append(splits[PATH]);
            if (!splits[PATH].equals("/"))
                buffer.append('/');
        }
        if (!splits[CONTENT].equals(""))
            buffer.append(splits[CONTENT]);
        if (!splits[ARGUMENTS].equals(""))
            buffer.append('?').append(splits[ARGUMENTS]);
        return buffer.toString();
    }

    public final static String mergeX_Host(String[] splits) {
        StringBuffer buffer = new StringBuffer();
        if (!splits[HOST].equals(""))
            buffer.append(splits[HOST]);
        if (!splits[PORT].equals(""))
            buffer.append(':').append(splits[PORT]);
        return buffer.toString();
    }


}
