package net.smelly.seekercompass.mixin.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.smelly.seekercompass.interfaces.Stalker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererManagerMixin {
	@Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
	private <E extends Entity> void shouldRender(E entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
		if (Stalker.getClientInstance().getStalkingEntity() == entity) info.setReturnValue(true);
	}
}
