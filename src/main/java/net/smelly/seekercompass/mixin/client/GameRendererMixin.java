package net.smelly.seekercompass.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.smelly.seekercompass.SCConfig;
import net.smelly.seekercompass.SeekerCompass;
import net.smelly.seekercompass.interfaces.Stalker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {

	@Inject(at = @At(value = "JUMP", ordinal = 1), method = "checkEntityPostEffect", cancellable = true)
	private void checkEntityPostEffect(@Nullable Entity entity, CallbackInfo info) {
		if (SCConfig.CLIENT.enableStalkingShader) {
			Stalker stalker = Stalker.getClientInstance();
			if (stalker != null) {
				LivingEntity stalkingEntity = stalker.getStalkingEntity();
				if (stalkingEntity != null && stalkingEntity == entity) {
					((GameRenderer) (Object) this).loadEffect(new ResourceLocation(SeekerCompass.MOD_ID, "shaders/post/seeker.json"));
					info.cancel();
				}
			}
		}
	}


}
