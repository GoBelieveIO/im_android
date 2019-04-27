/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

import android.text.TextUtils;
import com.beetle.bauhinia.db.IMessage;
import com.google.gson.JsonObject;

import java.util.UUID;

public  class Location extends MessageContent {
    public float latitude;
    public float longitude;
    public String address;
    public MessageType getType() { return MessageType.MESSAGE_LOCATION; }


    public static Location newLocation(float latitude, float longitude) {
        return newLocation(latitude, longitude, "");
    }

    public static Location newLocation(float latitude, float longitude, String address) {
        Location location = new Location();
        String uuid = UUID.randomUUID().toString();

        JsonObject content = new JsonObject();
        JsonObject locationJson = new JsonObject();
        locationJson.addProperty("latitude", latitude);
        locationJson.addProperty("longitude", longitude);
        if (!TextUtils.isEmpty(address)) {
            locationJson.addProperty("address", address);
        }
        content.add(LOCATION, locationJson);
        content.addProperty("uuid", uuid);
        location.raw = content.toString();
        location.longitude = longitude;
        location.latitude = latitude;
        location.address = address;
        location.uuid = uuid;
        return location;
    }
}
