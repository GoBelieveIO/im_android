/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.im;

/**
 * Created by houxh on 16/1/19.
 */
public class CustomerMessage {
    //未被序列化
    public long msgLocalID;

    public long customerAppID;
    public long customerID;
    public long storeID;
    public long sellerID;
    public int timestamp;
    public String content;

    //消息由本设备发出，则不需要重新入库，用于纠正消息标志位
    public boolean isSelf;
}
