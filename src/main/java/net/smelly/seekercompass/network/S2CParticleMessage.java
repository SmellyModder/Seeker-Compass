package net.smelly.seekercompass.network;

import java.util.function.Supplier;

import com.minecraftabnormals.abnormals_core.client.ClientInfo;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Message for telling the client to spawn particles
 * @author - SmellyModder(Luke Tonon)
 */
public final class S2CParticleMessage {
	private String particleName;
	private double posX, posY, posZ;
	private double motionX, motionY, motionZ;
	
	public S2CParticleMessage(String particleName, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		this.particleName = particleName;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
	}
	
	public void serialize(PacketBuffer buf) {
		buf.writeUtf(this.particleName);
		buf.writeDouble(this.posX);
		buf.writeDouble(this.posY);
		buf.writeDouble(this.posZ);
		buf.writeDouble(this.motionX);
		buf.writeDouble(this.motionY);
		buf.writeDouble(this.motionZ);
	}
	
	public static S2CParticleMessage deserialize(PacketBuffer buf) {
		String particleName = buf.readUtf();
		double posX = buf.readDouble();
		double posY = buf.readDouble();
		double posZ = buf.readDouble();
		double motionX = buf.readDouble();
		double motionY = buf.readDouble();
		double motionZ = buf.readDouble();
		return new S2CParticleMessage(particleName, posX, posY, posZ, motionX, motionY, motionZ);
	}
	
	public static boolean handle(S2CParticleMessage message, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
			context.enqueueWork(() -> {
				World world = ClientInfo.getClientPlayer().level;
				BasicParticleType particleType = (BasicParticleType) ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(message.particleName));
				
				if (particleType != null) {
					world.addParticle(particleType, message.posX, message.posY, message.posZ, message.motionX, message.motionY, message.motionZ);
				}
			});
		}
		context.setPacketHandled(true);
		return true;
	}
}