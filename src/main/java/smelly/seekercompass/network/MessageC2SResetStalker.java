package smelly.seekercompass.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import smelly.seekercompass.Stalker;

import java.util.function.Supplier;

public final class MessageC2SResetStalker {
    private final int entityId;

    public MessageC2SResetStalker(int entityId) {
        this.entityId = entityId;
    }

    public void serialize(PacketBuffer buf) {
        buf.writeInt(this.entityId);
    }

    public static MessageC2SResetStalker deserialize(PacketBuffer buf) {
        return new MessageC2SResetStalker(buf.readInt());
    }

    public static boolean handle(MessageC2SResetStalker message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
            context.enqueueWork(() -> {
                Entity entity = context.getSender().world.getEntityByID(message.entityId);
                if (entity instanceof Stalker) {
                    ((Stalker) entity).setStalkingEntity(null);
                }
            });
        }
        context.setPacketHandled(true);
        return true;
    }
}