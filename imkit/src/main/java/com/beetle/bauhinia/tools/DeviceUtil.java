/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

public class DeviceUtil {
	public static boolean isFullStorage() {
		if (!isMediaMounted())
			return true;

		File path = Environment.getExternalStorageDirectory();
		// 取得sdcard文件路径
		StatFs statfs = new StatFs(path.getPath());
		// 获取block的SIZE
		long blocSize = statfs.getBlockSize();
		// 己使用的Block的数量
		long availaBlock = statfs.getAvailableBlocks();
		return availaBlock * blocSize < 1048576;
	}

	public static boolean isMediaMounted() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}
}
