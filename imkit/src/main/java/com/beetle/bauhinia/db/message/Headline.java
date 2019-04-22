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

public  class Headline extends Notification {
    public String headline;

    public String getDescription() {
        return this.headline;
    }
    public MessageType getType() {
        return MessageType.MESSAGE_HEADLINE;
    }



    public static Headline newHeadline(String headline) {
        Headline head = new Headline();
        JsonObject content = new JsonObject();
        JsonObject headlineJson = new JsonObject();
        headlineJson.addProperty("headline", headline);
        content.add(HEADLINE, headlineJson);
        head.raw = content.toString();
        head.headline = headline;
        head.description = headline;
        return head;
    }

}
