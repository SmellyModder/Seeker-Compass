package net.smelly.seekercompass.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.smelly.seekercompass.interfaces.Stalker;

import java.util.function.Supplier;

public final class C2SStopStalkingMessage {

	public void serialize(PacketBuffer buf) {}

	public static C2SStopStalkingMessage deserialize(PacketBuffer buf) {
		return new C2SStopStalkingMessage();
	}

	public static void handle(C2SStopStalkingMessage message, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
			context.enqueueWork(() -> {
				ServerPlayerEntity sender = context.getSender();
				if (sender instanceof Stalker) {
					((Stalker) sender).setStalkingEntity(null);
				}
			});
		}
		context.setPacketHandled(true);
	}

}
