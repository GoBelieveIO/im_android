/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;

import android.util.Log;

import com.beetle.im.BytePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 16/1/18.
 * FileCustomerMessageDB vs SQLCustomerMessageDB
 */
public class CustomerMessageDB extends SQLCustomerMessageDB {
    public static final boolean SQL_ENGINE_DB = true;
    
    private static CustomerMessageDB instance = new CustomerMessageDB();

    public static CustomerMessageDB getInstance() {
        return instance;
    }

}
