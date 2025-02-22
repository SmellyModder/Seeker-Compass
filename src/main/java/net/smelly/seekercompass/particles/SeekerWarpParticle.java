package net.smelly.seekercompass.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * @author SmellyModder(Luke Tonon)
 */
@OnlyIn(Dist.CLIENT)
public class SeekerWarpParticle extends TextureSheetParticle {
	private final SpriteSet animatedSprite;
	private final float scale;

	public SeekerWarpParticle(SpriteSet animatedSprite, ClientLevel level, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		super(level, posX, posY, posZ, motionX, motionY, motionZ);
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
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		float f = ((float) this.age + partialTicks) / (float) this.lifetime;
		this.quadSize = this.scale * (1f - f * f * 0.5f);

		Vec3 vec3d = camera.getPosition();
		float f1 = (float)(Mth.lerp(partialTicks, this.xo, this.x) - vec3d.x());
		float f2 = (float)(Mth.lerp(partialTicks, this.yo, this.y) - vec3d.y());
		float f3 = (float)(Mth.lerp(partialTicks, this.zo, this.z) - vec3d.z());

		Quaternionf quaternion = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
		quaternion.rotateX(Mth.HALF_PI);

		Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
		float quadSize = this.getQuadSize(partialTicks);

		for (int i = 0; i < 4; ++i) {
			Vector3f vector3f = avector3f[i];
			vector3f.rotate(quaternion);
			vector3f.mul(quadSize);
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
		if (this.isAlive()) this.setSpriteFromAge(this.animatedSprite);
	}
	
	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	@Override
	public int getLightColor(float partialTick) {
		return 240;
    }
	
	public static class Factory implements ParticleProvider<SimpleParticleType> {
		private SpriteSet animatedSprite;

		public Factory(SpriteSet animatedSprite) {
			this.animatedSprite = animatedSprite;
		}
    	
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SeekerWarpParticle(this.animatedSprite, level, x, y + 0.01F, z, xSpeed, ySpeed, zSpeed);
		}
	}
}