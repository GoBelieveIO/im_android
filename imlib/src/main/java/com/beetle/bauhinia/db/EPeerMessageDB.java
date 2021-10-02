package com.beetle.bauhinia.db;

public class EPeerMessageDB extends BasePeerMessageDB {
    private static EPeerMessageDB instance = new EPeerMessageDB();

    public static EPeerMessageDB getInstance() {
        return instance;
    }

    EPeerMessageDB() {
        secret = 1;
    }
}
