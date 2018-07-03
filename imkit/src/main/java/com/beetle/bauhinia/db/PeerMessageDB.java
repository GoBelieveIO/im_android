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
