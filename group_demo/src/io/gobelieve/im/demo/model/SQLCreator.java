/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo.model;

public class SQLCreator {
    //cid:peer_uid|group_id|store_id
    public final static String CONVERSATION = "CREATE TABLE IF NOT EXISTS \"conversation\" "
            + "(\"id\" INTEGER PRIMARY KEY NOT NULL , "
            + "\"cid\" INTEGER NOT NULL, "
            + "\"type\" INTEGER NOT NULL, "
            + "\"name\" VARCHAR(255), "
            + "\"state\" INTEGER DEFAULT 0, "
            + "\"unread\" INTEGER DEFAULT 0) ";


    public final static String CONVERSATION_IDX = "CREATE UNIQUE INDEX [cid_type_idx] On [conversation] ( [cid], [type] );";


    public final static String PEER_MESSAGE = "CREATE TABLE \"peer_message\" ( `id` INTEGER PRIMARY KEY AUTOINCREMENT, `peer` INTEGER NOT NULL, `secret` INTEGER DEFAULT 0, `sender` INTEGER NOT NULL, `receiver` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `content` TEXT, `uuid` TEXT );";

    public final static String GROUP_MESSAGE = "CREATE TABLE \"group_message\" ( `id` INTEGER PRIMARY KEY AUTOINCREMENT, `sender` INTEGER NOT NULL, `group_id` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `content` TEXT, `uuid` TEXT );";

    public final static String CUSTOMER_MESSAGE = "CREATE TABLE \"customer_message\" ( `id` INTEGER PRIMARY KEY AUTOINCREMENT, `customer_id` INTEGER NOT NULL, `customer_appid` INTEGER NOT NULL, `store_id` INTEGER NOT NULL, `seller_id` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `is_support` INTEGER NOT NULL, `content` TEXT, `uuid` TEXT );";

    public final static String PEER_MESSAGE_FTS = "CREATE VIRTUAL TABLE peer_message_fts USING fts4(content TEXT);";

    public final static String GROUP_MESSAGE_FTS = "CREATE VIRTUAL TABLE group_message_fts USING fts4(content TEXT);";

    public final static String CUSTOMER_MESSAGE_FTS = "CREATE VIRTUAL TABLE customer_message_fts USING fts4(content TEXT);";

    public final static String PEER_MESSAGE_IDX = "CREATE INDEX `peer_index` ON `peer_message` (`peer`, `secret`, `id`);";
    public final static String PEER_MESSAGE_UUID_IDX = "CREATE INDEX `peer_uuid_index` ON `peer_message` (`uuid`)";
    public final static String GROUP_MESSAGE_UUID_IDX = "CREATE INDEX `group_uuid_index` ON `group_message` (`uuid`)";
    public final static String CUSTOMER_MESSAGE_UUID_IDX = "CREATE INDEX `customer_uuid_index` ON `customer_message` (`uuid`)";
}
