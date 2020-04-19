/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.gallery.tool;

import java.io.File;

/**
 * Created by hillwind
 */
public class FileUtils {

    public static void mkdirIfNeed(File file) {
        if (file != null && !file.exists()) {
            mkdirIfNeed(file.getParentFile());
            file.mkdir();
        }
    }

}
