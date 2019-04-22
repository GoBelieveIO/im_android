/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;


public class EPeerMessageDB extends SQLPeerMessageDB {
    public static final boolean SQL_ENGINE_DB = true;

    private static EPeerMessageDB instance = new EPeerMessageDB();

    public static EPeerMessageDB getInstance() {
        return instance;
    }


    EPeerMessageDB() {
        secret = 1;
    }

}
