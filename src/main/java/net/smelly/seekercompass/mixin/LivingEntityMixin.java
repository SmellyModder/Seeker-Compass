package net.smelly.seekercompass.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.smelly.seekercompass.interfaces.Stalkable;
import net.smelly.seekercompass.interfaces.Stalker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(LivingEntity.class)
public final class LivingEntityMixin implements Stalkable {
	private final Set<PlayerEntity> stalkers = new HashSet<>();
	private boolean dirty;

	@Inject(at = @At("HEAD"), method = "tick")
	private void tickStalking(CallbackInfo info) {
		if (!((LivingEntity) (Object) this).level.isClientSide) {
			Set<PlayerEntity> stalkers = this.stalkers;
			int prevSize = stalkers.size();
			stalkers.removeIf(player -> !player.isAlive() || ((Stalker) player).getStalkingEntity() != (Object) this);
			if (prevSize != stalkers.size()) {
				this.setDirty(true);
			}
		}
	}

	@Override
	public void addStalker(PlayerEntity player) {
		if (this.stalkers.add(player)) {
			this.setDirty(true);
		}
	}

	@Override
	public void removeStalker(PlayerEntity player) {
		if (this.stalkers.remove(player)) {
			this.setDirty(true);
		}
	}

	@Override
	public boolean hasStalkers() {
		return !this.stalkers.isEmpty();
	}

	@Override
	public boolean isBeingStalkedBy(PlayerEntity player) {
		return this.stalkers.contains(player);
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public boolean isDirty() {
		return this.dirty;
	}
}
