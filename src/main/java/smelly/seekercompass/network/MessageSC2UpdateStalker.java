package smelly.seekercompass.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import smelly.seekercompass.Stalker;

import java.util.function.Supplier;

public final class MessageSC2UpdateStalker {
    private final int entityId;

    public MessageSC2UpdateStalker(int entityId) {
        this.entityId = entityId;
    }

    public MessageSC2UpdateStalker() {
        this.entityId = -1;
    }

    public void serialize(PacketBuffer buf) {
        buf.writeInt(this.entityId);
    }

    public static MessageSC2UpdateStalker deserialize(PacketBuffer buf) {
        return new MessageSC2UpdateStalker(buf.readInt());
    }

    public static boolean handle(MessageSC2UpdateStalker message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                ClientPlayerEntity player = Minecraft.getInstance().player;
                int id = message.entityId;
                if (id >= 0) {
                    Entity entity = player.world.getEntityByID(message.entityId);
                    if (entity instanceof LivingEntity) {
                        ((Stalker) player).setStalkingEntity((LivingEntity) entity);
                    }
                } else {
                    ((Stalker) player).setStalkingEntity(null);
                }
            });
        }
        context.setPacketHandled(true);
        return true;
    }
}