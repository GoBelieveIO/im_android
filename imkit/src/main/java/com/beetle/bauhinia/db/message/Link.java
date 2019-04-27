/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

import com.beetle.bauhinia.db.IMessage;

public  class Link extends MessageContent {
    public String title;
    public String content;
    public String url;
    public String image;
    public MessageType getType() { return MessageType.MESSAGE_LINK; }
}
