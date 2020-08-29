package smelly.seekercompass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class MessageSC2UpdateStalker {
    private final int entityId;

    public MessageSC2UpdateStalker(int entityId) {
        this.entityId = entityId;
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
                Entity entity = player.world.getEntityByID(message.entityId);
                if (entity instanceof LivingEntity) {
                    ((Stalker) player).setStalkingEntity((LivingEntity) entity);
                }
            });
        }
        context.setPacketHandled(true);
        return true;
    }
}