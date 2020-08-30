package smelly.seekercompass.mixin;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smelly.seekercompass.Stalker;

@Mixin(ClientPlayerEntity.class)
public final class ClientPlayerEntityMixin {

    @Inject(at = @At("HEAD"), method = "isCurrentViewEntity", cancellable = true)
    private void isCurrentViewEntity(CallbackInfoReturnable<Boolean> info) {
        if (((Stalker) (Object) this).getStalkingEntity() != null) info.setReturnValue(false);
    }

}