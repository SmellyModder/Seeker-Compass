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
		this.scale = this.quadSize = 1.0F;
		this.rCol = 1.0F;
		this.gCol = 1.0F;
		this.bCol = 1.0F;
		this.xd = this.yd = this.zd = 0.0F;
		this.lifetime = 20;
		this.animatedSprite = animatedSprite;
		this.setSpriteFromAge(animatedSprite);
	}
	
	@Override
	public void render(IVertexBuilder buffer, ActiveRenderInfo activeInfo, float partialTicks) {
		float f = ((float) this.age + partialTicks) / (float) this.lifetime;
		this.quadSize = this.scale * (1f - f * f * 0.5f);
		
		Vector3d vec3d = activeInfo.getPosition();
		float f1 = (float)(MathHelper.lerp(partialTicks, this.xo, this.x) - vec3d.x());
		float f2 = (float)(MathHelper.lerp(partialTicks, this.yo, this.y) - vec3d.y());
		float f3 = (float)(MathHelper.lerp(partialTicks, this.zo, this.z) - vec3d.z());
		
		Quaternion quaternion = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
		quaternion.mul(Vector3f.XP.rotationDegrees(90.0F));
		
		Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
		vector3f1.transform(quaternion);
		Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
		float f4 = this.getQuadSize(partialTicks);

		for(int i = 0; i < 4; ++i) {
			Vector3f vector3f = avector3f[i];
			vector3f.transform(quaternion);
			vector3f.mul(f4);
			vector3f.add(f1, f2, f3);
		}

		float f7 = this.getU0();
		float f8 = this.getU1();
		float f5 = this.getV0();
		float f6 = this.getV1();
		int j = this.getLightColor(partialTicks);
		buffer.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).uv(f8, f6).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
		buffer.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).uv(f8, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
		buffer.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).uv(f7, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
		buffer.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).uv(f7, f6).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
	}
	
	@Override
    public void tick() {
		super.tick();
		this.oRoll = this.roll;
		
		if(this.isAlive()) this.setSpriteFromAge(this.animatedSprite);
	}
	
	@Override
	public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	@Override
	public int getLightColor(float partialTick) {
		return 240;
    }
	
	public static class Factory implements IParticleFactory<BasicParticleType> {
		private IAnimatedSprite animatedSprite;

		public Factory(IAnimatedSprite animatedSprite) {
			this.animatedSprite = animatedSprite;
		}
    	
		@Override
		public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SeekerWarpParticle(this.animatedSprite, world, x, y + 0.01F, z, xSpeed, ySpeed, zSpeed);
		}
	}
}