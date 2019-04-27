/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;
import java.util.UUID;

public class Image extends MessageContent {
    public String url;
    public int width;
    public int height;
    public MessageType getType() {
        return MessageType.MESSAGE_IMAGE;
    }





    public static Image newImage(String url, int width, int height, String uuid) {
        Image image = new Image();

        JsonObject content = new JsonObject();
        //兼容性
        content.addProperty(IMAGE, url);
        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        content.add(IMAGE2, obj);
        content.addProperty("uuid", uuid);
        image.raw = content.toString();
        image.url = url;
        image.width = width;
        image.height = height;
        image.uuid = uuid;
        return image;
    }

    public static Image newImage(String url, int width, int height) {
        String uuid = UUID.randomUUID().toString();
        return newImage(url, width, height, uuid);
    }

}
