package org.sosly.villagetale.network;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.sosly.villagetale.VillageTale;

public class ServerPacketHandler {
    public static <T extends BasePacket> boolean validateBasics(T message, NetworkEvent.Context ctx) {
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Logger logger = VillageTale.LOGGER;
            String messageName = message.getClass().getName();
            logger.error(messageName + " received on wrong side: " + sideReceived);
            return false;
        }

        if (!message.isMessageValid()) {
            Logger logger = VillageTale.LOGGER;
            String messageName = message.getClass().getName();
            logger.error(messageName + " was invalid: " + message);
            return false;
        }

        return true;
    }
}
