/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

public class Revoke extends Notification {
    public String msgid;

    public static Revoke newRevoke(String msgid) {
        Revoke revoke = new Revoke();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("msgid", msgid);
        content.add(REVOKE, json);

        revoke.setRaw(content.toString());
        revoke.msgid = msgid;
        return revoke;
    }

    public MessageType getType() {
        return MessageType.MESSAGE_REVOKE;
    }
}
