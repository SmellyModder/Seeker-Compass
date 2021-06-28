package net.smelly.seekercompass.mixin.client;

import net.minecraft.client.Minecraft;
import net.smelly.seekercompass.SeekerCompass;
import net.smelly.seekercompass.interfaces.Stalker;
import net.smelly.seekercompass.network.C2SStopStalkingMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public final class MinecraftMixin {
	@Shadow
	private int rightClickDelay;

	@Inject(at = @At("HEAD"), method = "startUseItem", cancellable = true)
	private void stopStalking(CallbackInfo info) {
		if (Stalker.getClientInstance().isStalking()) {
			this.rightClickDelay = 4;
			SeekerCompass.CHANNEL.sendToServer(new C2SStopStalkingMessage());
			info.cancel();
		}
	}
}
