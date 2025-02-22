package net.smelly.seekercompass.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StalkingSound extends AbstractSoundInstance {

	public StalkingSound(boolean activate) {
		super(SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.looping = false;
		this.volume = 0.5F;
		this.pitch = (activate ? 0.75F : 0.6F);
		this.relative = true;
	}

}
