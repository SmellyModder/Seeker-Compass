package smelly.seekercompass.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smelly.seekercompass.Stalker;

@Mixin(EntityRendererManager.class)
public final class EntityRendererManagerMixin {
    private static final Minecraft MC = Minecraft.getInstance();

    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private <E extends Entity> void shouldRender(E entity, ClippingHelper clippingHelper, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> info) {
        if (((Stalker) MC.player).getStalkingEntity() == entity) info.setReturnValue(false);
    }
}