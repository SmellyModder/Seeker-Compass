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
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author SmellyModder(Luke Tonon)
 */
@OnlyIn(Dist.CLIENT)
public class SeekerWarpParticle extends SpriteTexturedParticle {
	private final IAnimatedSprite animatedSprite;
	private final float scale;

	public SeekerWarpParticle(IAnimatedSprite animatedSprite, ClientWorld world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		super(world, posX, posY, posZ, motionX, motionY, motionZ);
		this.scale = this.particleScale = 1.0F;
		this.particleRed = 1.0F;
		this.particleGreen = 1.0F;
		this.particleBlue = 1.0F;
		this.motionX = this.motionY = this.motionZ = 0.0F;
		this.maxAge = 20;
		this.animatedSprite = animatedSprite;
		this.selectSpriteWithAge(animatedSprite);
	}
	
	@Override
	public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo activeInfo, float partialTicks) {
		float f = ((float) this.age + partialTicks) / (float) this.maxAge;
		this.particleScale = this.scale * (1f - f * f * 0.5f);
		
		Vector3d vec3d = activeInfo.getProjectedView();
		float f1 = (float)(MathHelper.lerp(partialTicks, this.prevPosX, this.posX) - vec3d.getX());
		float f2 = (float)(MathHelper.lerp(partialTicks, this.prevPosY, this.posY) - vec3d.getY());
		float f3 = (float)(MathHelper.lerp(partialTicks, this.prevPosZ, this.posZ) - vec3d.getZ());
		
		Quaternion quaternion = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
		quaternion.multiply(Vector3f.XP.rotationDegrees(90.0F));
		
		Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
		vector3f1.transform(quaternion);
		Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
		float f4 = this.getScale(partialTicks);

		for(int i = 0; i < 4; ++i) {
			Vector3f vector3f = avector3f[i];
			vector3f.transform(quaternion);
			vector3f.mul(f4);
			vector3f.add(f1, f2, f3);
		}

		float f7 = this.getMinU();
		float f8 = this.getMaxU();
		float f5 = this.getMinV();
		float f6 = this.getMaxV();
		int j = this.getBrightnessForRender(partialTicks);
		buffer.pos(avector3f[0].getX(), avector3f[0].getY(), avector3f[0].getZ()).tex(f8, f6).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
		buffer.pos(avector3f[1].getX(), avector3f[1].getY(), avector3f[1].getZ()).tex(f8, f5).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
		buffer.pos(avector3f[2].getX(), avector3f[2].getY(), avector3f[2].getZ()).tex(f7, f5).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
		buffer.pos(avector3f[3].getX(), avector3f[3].getY(), avector3f[3].getZ()).tex(f7, f6).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
	}
	
	@Override
    public void tick() {
		super.tick();
		this.prevParticleAngle = this.particleAngle;
		
		if(this.isAlive()) this.selectSpriteWithAge(this.animatedSprite);
	}
	
	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	@Override
	public int getBrightnessForRender(float partialTick) {
		return 240;
    }
	
	public static class Factory implements IParticleFactory<BasicParticleType> {
		private IAnimatedSprite animatedSprite;

		public Factory(IAnimatedSprite animatedSprite) {
			this.animatedSprite = animatedSprite;
		}
    	
		@Override
		public Particle makeParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SeekerWarpParticle(this.animatedSprite, world, x, y + 0.01F, z, xSpeed, ySpeed, zSpeed);
		}
	}
}