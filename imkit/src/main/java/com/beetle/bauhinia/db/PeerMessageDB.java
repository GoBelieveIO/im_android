package com.beetle.bauhinia.db;


/**
 * Created by houxh on 14-7-22.
 *
 * FilePeerMessageDB vs SQLPeerMessageDB
 */
public class PeerMessageDB extends FilePeerMessageDB {

    private static PeerMessageDB instance = new PeerMessageDB();

    public static PeerMessageDB getInstance() {
        return instance;
    }

}
