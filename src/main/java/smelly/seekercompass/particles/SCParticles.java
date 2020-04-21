package smelly.seekercompass.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import smelly.seekercompass.SeekerCompass;

public class SCParticles {
	public static final BasicParticleType SEEKER_EYES = createBasicParticleType(true, "seeker_eyes");
	
	private static BasicParticleType createBasicParticleType(boolean alwaysShow, String name) {
		BasicParticleType particleType = new BasicParticleType(alwaysShow);
		particleType.setRegistryName(SeekerCompass.MOD_ID, name);
		return particleType;
	}
	
	@Mod.EventBusSubscriber(modid = SeekerCompass.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegisterParticleTypes {
		
		@SubscribeEvent
		public static void registerParticleTypes(RegistryEvent.Register<ParticleType<?>> event) {
			event.getRegistry().registerAll(
				SEEKER_EYES
			);
		}
		
	}
	
	@EventBusSubscriber(modid = SeekerCompass.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
	public static class RegisterParticleFactories {
		
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void registerParticleTypes(ParticleFactoryRegisterEvent event) {
			Minecraft MC = Minecraft.getInstance();
			MC.particles.registerFactory(SEEKER_EYES, SeekerEyesParticle.Factory::new);
		}
		
	}
}