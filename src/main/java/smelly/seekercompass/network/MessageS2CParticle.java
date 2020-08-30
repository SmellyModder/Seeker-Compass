package smelly.seekercompass.network;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
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
public class MessageS2CParticle {
	private String particleName;
	private double posX, posY, posZ;
	private double motionX, motionY, motionZ;
	
	public MessageS2CParticle(String particleName, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		this.particleName = particleName;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.motionX = motionX;
		this.motionY = motionY;
		this.motionZ = motionZ;
	}
	
	public void serialize(PacketBuffer buf) {
		buf.writeString(this.particleName);
		buf.writeDouble(this.posX);
		buf.writeDouble(this.posY);
		buf.writeDouble(this.posZ);
		buf.writeDouble(this.motionX);
		buf.writeDouble(this.motionY);
		buf.writeDouble(this.motionZ);
	}
	
	public static MessageS2CParticle deserialize(PacketBuffer buf) {
		String particleName = buf.readString();
		double posX = buf.readDouble();
		double posY = buf.readDouble();
		double posZ = buf.readDouble();
		double motionX = buf.readDouble();
		double motionY = buf.readDouble();
		double motionZ = buf.readDouble();
		return new MessageS2CParticle(particleName, posX, posY, posZ, motionX, motionY, motionZ);
	}
	
	public static boolean handle(MessageS2CParticle message, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
			context.enqueueWork(() -> {
				World world = Minecraft.getInstance().player.world;
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