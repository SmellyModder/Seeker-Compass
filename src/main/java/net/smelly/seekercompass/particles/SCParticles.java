package net.smelly.seekercompass.particles;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.smelly.seekercompass.SeekerCompass;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class SCParticles {
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SeekerCompass.MOD_ID);
	public static final RegistryObject<SimpleParticleType> SEEKER_EYES = register("seeker_eyes");
	public static final RegistryObject<SimpleParticleType> SEEKER_WARP = register("seeker_warp");

	private static RegistryObject<SimpleParticleType> register(String name) {
		return PARTICLES.register(name, () -> new SimpleParticleType(false));
	}
	
	@EventBusSubscriber(modid = SeekerCompass.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class RegisterParticleFactories {

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void registerParticleTypes(RegisterParticleProvidersEvent event) {
			event.registerSpriteSet(SEEKER_EYES.get(), SeekerEyesParticle.Factory::new);
			event.registerSpriteSet(SEEKER_WARP.get(), SeekerWarpParticle.Factory::new);
		}

	}
}