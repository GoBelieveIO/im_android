/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.im;

/**
 * Created by houxh on 2016/11/2.
 */

public interface SyncKeyHandler {
    boolean saveSyncKey(long syncKey);
    boolean saveGroupSyncKey(long groupID, long syncKey);
}
