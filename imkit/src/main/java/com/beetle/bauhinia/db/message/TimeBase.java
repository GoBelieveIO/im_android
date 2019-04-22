/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;


import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

public  class TimeBase extends Notification {
    public int timestamp;
    public MessageType getType() {
        return MessageType.MESSAGE_TIME_BASE;
    }


    public static TimeBase newTimeBase(int timestamp) {
        TimeBase tb = new TimeBase();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("timestamp", timestamp);
        content.add(TIMEBASE, json);
        tb.raw = content.toString();
        tb.timestamp = timestamp;
        return tb;
    }

}
