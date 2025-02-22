package net.smelly.seekercompass.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.smelly.seekercompass.interfaces.ClientStalkable;

import java.util.function.Supplier;

public final class S2CUpdateStalkedMessage {
	private final int entityId;
	private final boolean beingStalked;

	public S2CUpdateStalkedMessage(int entityId, boolean beingStalked) {
		this.entityId = entityId;
		this.beingStalked = beingStalked;
	}

	public void serialize(FriendlyByteBuf buf) {
		buf.writeInt(this.entityId);
		buf.writeBoolean(this.beingStalked);
	}

	public static S2CUpdateStalkedMessage deserialize(FriendlyByteBuf buf) {
		return new S2CUpdateStalkedMessage(buf.readInt(), buf.readBoolean());
	}

	public static void handle(S2CUpdateStalkedMessage message, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
			context.enqueueWork(() -> {
				Entity entity = Minecraft.getInstance().player.level().getEntity(message.entityId);
				if (entity instanceof ClientStalkable) {
					((ClientStalkable) entity).setBeingStalked(message.beingStalked);
				}
			});
		}
		context.setPacketHandled(true);
	}
}
