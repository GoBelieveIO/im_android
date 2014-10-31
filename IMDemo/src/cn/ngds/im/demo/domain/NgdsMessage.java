package cn.ngds.im.demo.domain;

import com.gameservice.sdk.im.IMMessage;

/**
 * Message
 * Description:消息
 */
public class NgdsMessage {
    public static enum Direct {
        SEND, RECEIVE
    }


    public IMMessage mIMMessage;
    public Direct mDirection;
    public long time;
    public boolean serverReceived;
    public boolean receiverReceived;
    public boolean sendFailure;

    public NgdsMessage(IMMessage IMMessage, Direct direction) {
        mIMMessage = IMMessage;
        mDirection = direction;
        if (mDirection == Direct.RECEIVE) {
            time = mIMMessage.timestamp*1000L;
        } else {
            time = System.currentTimeMillis();
        }
    }


    public boolean isDirectSend() {
        return mDirection == Direct.SEND;
    }
}
