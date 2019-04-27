/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;



public class MessageFlag {
    public static final int MESSAGE_FLAG_DELETE = 1;
    public static final int MESSAGE_FLAG_ACK = 2;
    //public static final int MESSAGE_FLAG_PEER_ACK = 4;
    public static final int MESSAGE_FLAG_FAILURE = 8;
    public static final int MESSAGE_FLAG_LISTENED = 16;
}
