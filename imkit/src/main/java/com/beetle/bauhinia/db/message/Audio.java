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

import java.util.UUID;

public  class Audio extends MessageContent {
    public String url;
    public long duration;


    public static Audio newAudio(String url, long duration, String uuid) {
        Audio audio = new Audio();


        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", duration);
        audioJson.addProperty("url", url);
        content.add(AUDIO, audioJson);
        content.addProperty("uuid", uuid);
        audio.raw = content.toString();

        audio.duration = duration;
        audio.url = url;
        audio.uuid = uuid;
        return audio;
    }


    public static Audio newAudio(String url, long duration) {
        String uuid = UUID.randomUUID().toString();
        return newAudio(url, duration, uuid);
    }


    public MessageType getType() {
        return MessageType.MESSAGE_AUDIO;
    }
}
