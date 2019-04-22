/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.api.body;

import com.google.gson.annotations.SerializedName;

/**
 * Created by houxh on 15/2/2.
 */
public class PostDeviceToken {
    @SerializedName("ng_device_token")
    public String deviceToken;

    @SerializedName("xg_device_token")
    public String xgDeviceToken;

    @SerializedName("xm_device_token")
    public String xmDeviceToken;

    @SerializedName("hw_device_token")
    public String hwDeviceToken;

    @SerializedName("gcm_device_token")
    public String gcmDeviceToken;

}
