package org.sosly.villagetale.network;

public abstract class BasePacket {
    protected boolean messageIsValid = false;

    public BasePacket() {
    }

    public final boolean isMessageValid() {
        return this.messageIsValid;
    }
}
