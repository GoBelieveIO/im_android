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
 *
 * FilePeerMessageDB vs SQLPeerMessageDB
 */
public class PeerMessageDB extends SQLPeerMessageDB {
    public static final boolean SQL_ENGINE_DB = true;

    private static PeerMessageDB instance = new PeerMessageDB();

    public static PeerMessageDB getInstance() {
        return instance;
    }


    PeerMessageDB() {
        secret = 0;
    }

}
