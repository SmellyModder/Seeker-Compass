package smelly.seekercompass.particles;

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

/**
 * @author SmellyModder(Luke Tonon)
 */
@OnlyIn(Dist.CLIENT)
public class SeekerEyesParticle extends SpriteTexturedParticle {
	protected final IAnimatedSprite animatedSprite;
	private final float scale;

	public SeekerEyesParticle(IAnimatedSprite animatedSprite, ClientWorld world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		super(world, posX, posY, posZ, motionX, motionY, motionZ);
		this.scale = this.particleScale = this.rand.nextFloat() * 0.6F + 0.2F;
		this.particleRed = 1.0F;
		this.particleGreen = 1.0F;
		this.particleBlue = 1.0F;
		this.motionX = motionX * (double)0.2F + (Math.random() * 2.0D - 1.0D) * (double)0.01F;
		this.motionY = motionY * (double)0.2F + (Math.random() * 2.0D - 1.0D) * (double)0.01F;
		this.motionZ = motionZ * (double)0.2F + (Math.random() * 2.0D - 1.0D) * (double)0.01F;
		this.maxAge = 20;
		this.animatedSprite = animatedSprite;
		this.selectSpriteWithAge(animatedSprite);
	}
	
	@Override
	public void renderParticle(IVertexBuilder p_225606_1_, ActiveRenderInfo activeInfo, float partialTicks) {
		float f = ((float) this.age + partialTicks) / (float) this.maxAge;
		this.particleScale = this.scale * (1f - f * f * 0.5f);
		super.renderParticle(p_225606_1_, activeInfo, partialTicks);
	}
	
	@Override
    public void tick() {
		super.tick();
		this.prevParticleAngle = this.particleAngle;
		
		if(this.isAlive()) {
			this.selectSpriteWithAge(this.animatedSprite);
		}
	}
	
	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	@Override
	public int getBrightnessForRender(float partialTick) {
		float f = ((float) this.age + partialTick) / (float) this.maxAge;
		f = MathHelper.clamp(f, 0f, 1f);
		int i = super.getBrightnessForRender(partialTick);
		int j = i & 255;
		int k = i >> 16 & 255;
		j = j + (int) (f * 15f * 16f);
		if(j > 240) {
			j = 240;
		}
		return j | k << 16;
    }
	
	public static class Factory implements IParticleFactory<BasicParticleType> {
		private IAnimatedSprite animatedSprite;

		public Factory(IAnimatedSprite animatedSprite) {
			this.animatedSprite = animatedSprite;
		}
    	
		@Override
		public Particle makeParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SeekerEyesParticle(this.animatedSprite, world, x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}
}