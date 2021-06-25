package net.smelly.seekercompass.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * @author SmellyModder(Luke Tonon)
 */
@OnlyIn(Dist.CLIENT)
public class SeekerEyesParticle extends SpriteTexturedParticle {
	private final IAnimatedSprite animatedSprite;
	private final float scale;

	public SeekerEyesParticle(IAnimatedSprite animatedSprite, ClientWorld world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		super(world, posX, posY, posZ, motionX, motionY, motionZ);
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
	public void render(IVertexBuilder p_225606_1_, ActiveRenderInfo activeInfo, float partialTicks) {
		float f = ((float) this.age + partialTicks) / (float) this.lifetime;
		this.quadSize = this.scale * (1f - f * f * 0.5f);
		super.render(p_225606_1_, activeInfo, partialTicks);
	}
	
	@Override
    public void tick() {
		super.tick();
		this.oRoll = this.roll;
		
		if (this.isAlive()) this.setSpriteFromAge(this.animatedSprite);
	}
	
	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	@Override
	public int getLightColor(float partialTick) {
		float f = ((float) this.age + partialTick) / (float) this.lifetime;
		f = MathHelper.clamp(f, 0f, 1f);
		int i = super.getLightColor(partialTick);
		int j = i & 255;
		int k = i >> 16 & 255;
		j = j + (int) (f * 15f * 16f);
		if (j > 240) {
			j = 240;
		}
		return j | k << 16;
    }
	
	public static class Factory implements IParticleFactory<BasicParticleType> {
		private IAnimatedSprite animatedSprite;

		public Factory(IAnimatedSprite animatedSprite) {
			this.animatedSprite = animatedSprite;
		}

		@Nullable
		@Override
		public Particle createParticle(BasicParticleType basicParticleType, ClientWorld world, double v, double v1, double v2, double v3, double v4, double v5) {
			return new SeekerEyesParticle(this.animatedSprite, world, v, v1, v2, v3, v4, v5);
		}
	}
}