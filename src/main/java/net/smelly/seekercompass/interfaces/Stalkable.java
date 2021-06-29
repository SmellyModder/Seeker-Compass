package net.smelly.seekercompass.interfaces;

import net.minecraft.entity.player.PlayerEntity;

public interface Stalkable {
	void addStalker(PlayerEntity player);

	void removeStalker(PlayerEntity player);

	boolean hasStalkers();

	boolean isBeingStalkedBy(PlayerEntity player);

	void setDirty(boolean dirty);

	boolean isDirty();
}
