package net.smelly.seekercompass.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author SmellyModder(Luke Tonon)
 */
@OnlyIn(Dist.CLIENT)
public class SeekerEyesParticle extends TextureSheetParticle {
	private final SpriteSet animatedSprite;
	private final float scale;

	public SeekerEyesParticle(SpriteSet animatedSprite, ClientLevel level, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		super(level, posX, posY, posZ, motionX, motionY, motionZ);
		this.scale = this.quadSize = this.random.nextFloat() * 0.6F + 0.2F;
		this.rCol = 1.0F;
		this.gCol = 1.0F;
		this.bCol = 1.0F;
		this.xd = motionX * (double)0.2F + (Math.random() * 2.0D - 1.0D) * (double)0.01F;
		this.yd = motionY * (double)0.2F + (Math.random() * 2.0D - 1.0D) * (double)0.01F;
		this.zd = motionZ * (double)0.2F + (Math.random() * 2.0D - 1.0D) * (double)0.01F;
		this.lifetime = 20;
		this.animatedSprite = animatedSprite;
		this.setSpriteFromAge(animatedSprite);
	}

	@Override
	public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
		float f = ((float) this.age + partialTicks) / (float) this.lifetime;
		this.quadSize = this.scale * (1f - f * f * 0.5f);
		super.render(consumer, camera, partialTicks);
	}
	
	@Override
    public void tick() {
		super.tick();
		this.oRoll = this.roll;
		if (this.isAlive()) this.setSpriteFromAge(this.animatedSprite);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	@Override
	public int getLightColor(float partialTick) {
		float f = ((float) this.age + partialTick) / (float) this.lifetime;
		f = Mth.clamp(f, 0f, 1f);
		int i = super.getLightColor(partialTick);
		int j = i & 255;
		int k = i >> 16 & 255;
		j = j + (int) (f * 15f * 16f);
		if (j > 240) {
			j = 240;
		}
		return j | k << 16;
    }
	
	public static class Factory implements ParticleProvider<SimpleParticleType> {
		private SpriteSet animatedSprite;

		public Factory(SpriteSet animatedSprite) {
			this.animatedSprite = animatedSprite;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SeekerEyesParticle(this.animatedSprite, world, x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}
}