package net.smelly.seekercompass.mixin.client;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.smelly.seekercompass.interfaces.Stalker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public final class ClientPlayerEntityMixin {

	@Inject(at = @At("HEAD"), method = "isHandsBusy", cancellable = true)
	private void isHandsBusy(CallbackInfoReturnable<Boolean> info) {
		if (((Stalker) (Object) this).isStalking()) {
			info.setReturnValue(true);
		}
	}

}
