/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

public class File extends MessageContent {
    public String url;
    public String filename;
    public int size;

    public MessageType getType() {
        return MessageType.MESSAGE_FILE;
    }

}
