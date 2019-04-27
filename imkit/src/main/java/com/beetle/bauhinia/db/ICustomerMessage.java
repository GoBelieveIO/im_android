/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;


/**
 * Created by houxh on 14-7-22.
 */


public class ICustomerMessage  extends IMessage {
    public long customerAppID;
    public long customerID;
    public long storeID;
    public long sellerID;
    public boolean isSupport;//是否发自客服人员
}
