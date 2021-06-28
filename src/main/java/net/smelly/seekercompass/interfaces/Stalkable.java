package net.smelly.seekercompass.interfaces;

import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nullable;

public interface Stalkable {
	void setStalker(@Nullable PlayerEntity player);

	@Nullable
	PlayerEntity getStalker();
}
