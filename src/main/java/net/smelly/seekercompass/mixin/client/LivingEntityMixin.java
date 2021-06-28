package net.smelly.seekercompass.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.smelly.seekercompass.interfaces.ClientStalkable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public final class LivingEntityMixin implements ClientStalkable {
	private boolean beingStalked;

	@Override
	public void setBeingStalked(boolean beingStalked) {
		this.beingStalked = beingStalked;
	}

	@Override
	public boolean isBeingStalked() {
		return this.beingStalked;
	}
}
