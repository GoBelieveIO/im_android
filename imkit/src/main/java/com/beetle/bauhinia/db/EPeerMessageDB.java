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
