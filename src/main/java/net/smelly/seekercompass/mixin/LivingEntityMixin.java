package net.smelly.seekercompass.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.smelly.seekercompass.interfaces.Stalkable;
import net.smelly.seekercompass.interfaces.Stalker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public final class LivingEntityMixin implements Stalkable {
	@Nullable
	private PlayerEntity stalker;

	@Inject(at = @At("HEAD"), method = "tick")
	private void tickStalking(CallbackInfo info) {
		if (!((LivingEntity) (Object) this).level.isClientSide) {
			PlayerEntity stalker = this.stalker;
			if (stalker != null && (!stalker.isAlive() || ((Stalker) stalker).getStalkingEntity() != (Object) this)) {
				this.setStalker(null);
			}
		}
	}

	@Override
	public void setStalker(@Nullable PlayerEntity player) {
		this.stalker = player;
	}

	@Nullable
	@Override
	public PlayerEntity getStalker() {
		return this.stalker;
	}
}
