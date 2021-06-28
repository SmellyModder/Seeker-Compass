package net.smelly.seekercompass.network;

import com.minecraftabnormals.abnormals_core.client.ClientInfo;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.smelly.seekercompass.interfaces.ClientStalkable;

import java.util.function.Supplier;

public final class S2CUpdateStalkedMessage {
	private final int entityId;
	private final boolean beingStalked;

	public S2CUpdateStalkedMessage(int entityId, boolean beingStalked) {
		this.entityId = entityId;
		this.beingStalked = beingStalked;
	}

	public void serialize(PacketBuffer buf) {
		buf.writeInt(this.entityId);
		buf.writeBoolean(this.beingStalked);
	}

	public static S2CUpdateStalkedMessage deserialize(PacketBuffer buf) {
		return new S2CUpdateStalkedMessage(buf.readInt(), buf.readBoolean());
	}

	public static void handle(S2CUpdateStalkedMessage message, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
			context.enqueueWork(() -> {
				ClientPlayerEntity player = ClientInfo.getClientPlayer();
				Entity entity = player.level.getEntity(message.entityId);
				if (entity instanceof ClientStalkable) {
					((ClientStalkable) entity).setBeingStalked(message.beingStalked);
				}
			});
		}
		context.setPacketHandled(true);
	}
}
