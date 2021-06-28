package net.smelly.seekercompass.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public interface Stalker {
	void setStalkingEntity(@Nullable LivingEntity stalkingEntity);

	@Nullable
	LivingEntity getStalkingEntity();

	boolean isStalking();

	void setShouldBeStalking(boolean canStalk);

	boolean shouldBeStalking();

	@Nullable
	@OnlyIn(Dist.CLIENT)
	static Stalker getClientInstance() {
		return (Stalker) Minecraft.getInstance().player;
	}
}
