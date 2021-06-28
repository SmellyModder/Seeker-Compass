package net.smelly.seekercompass.sound;

import net.minecraft.client.audio.LocatableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class StalkingSound extends LocatableSound {

	public StalkingSound(boolean activate) {
		super(SoundEvents.TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS);
		this.looping = false;
		this.volume = 0.5F;
		this.pitch = (activate ? 0.75F : 0.6F) - new Random().nextFloat() * 0.1F;
		this.priority = true;
		this.relative = true;
	}

}
