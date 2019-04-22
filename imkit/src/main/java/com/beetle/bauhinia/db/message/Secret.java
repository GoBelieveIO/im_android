/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

public class Secret extends MessageContent {
    public String ciphertext;
    public int type;


    public static Secret newSecret(String ciphertext, int type, String uuid) {
        Secret s = new Secret();

        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("ciphertext", ciphertext);
        audioJson.addProperty("type", type);
        content.add(SECRET, audioJson);
        content.addProperty("uuid", uuid);
        s.raw = content.toString();

        s.ciphertext = ciphertext;
        s.type = type;
        s.uuid = uuid;
        return s;
    }


    public MessageType getType() {
        return MessageType.MESSAGE_SECRET;
    }

}
