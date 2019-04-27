/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;

import android.util.Log;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 15/3/21.
 */
public interface MessageIterator {
    public IMessage next();
}

