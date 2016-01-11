package com.beetle.im;

/**
 * Created by houxh on 14-12-31.
 */
public class VOIPControl {
    public static final int VOIP_COMMAND_DIAL = 1;

    public static final int VOIP_COMMAND_ACCEPT = 2;
    public static final int VOIP_COMMAND_CONNECTED = 3;
    public static final int VOIP_COMMAND_REFUSE = 4;
    public static final int VOIP_COMMAND_REFUSED = 5;
    public static final int VOIP_COMMAND_HANG_UP = 6;
    public static final int VOIP_COMMAND_TALKING = 8;

    public static final int VOIP_COMMAND_DIAL_VIDEO = 9;

    public long sender;
    public long receiver;
    public int cmd;
    public int dialCount;//只对VOIP_COMMAND_DIAL, VOIP_COMMAND_DIAL_VIDEO有意义


    public NatPortMap natMap; //VOIP_COMMAND_ACCEPT，VOIP_COMMAND_CONNECTED
    public static class NatPortMap {
        public int ip;
        public short port;
    }

    public int relayIP;//VOIP_COMMAND_CONNECTED
}