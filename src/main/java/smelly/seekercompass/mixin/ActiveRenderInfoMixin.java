package smelly.seekercompass.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import smelly.seekercompass.Stalker;

@Mixin(ActiveRenderInfo.class)
public abstract class ActiveRenderInfoMixin {
    private static final Minecraft MC = Minecraft.getInstance();

    @Shadow
    private boolean thirdPerson;
    @Shadow
    private float height;
    @Shadow
    private float previousHeight;

    @Inject(at = @At("RETURN"), method = "update")
    public void update(IBlockReader worldIn, Entity renderViewEntity, boolean thirdPersonIn, boolean thirdPersonReverseIn, float partialTicks, CallbackInfo info) {
        LivingEntity stalkingEntity = ((Stalker) MC.player).getStalkingEntity();
        if (stalkingEntity != null) {
            MC.gameSettings.hideGUI = true;
            this.thirdPerson = true;
            this.callSetDirection(stalkingEntity.getYaw(partialTicks), stalkingEntity.getPitch(partialTicks));
            this.callSetPosition(MathHelper.lerp(partialTicks, stalkingEntity.prevPosX, stalkingEntity.getPosX()), MathHelper.lerp(partialTicks, stalkingEntity.prevPosY, stalkingEntity.getPosY()) + (double) MathHelper.lerp(partialTicks, this.previousHeight, this.height), MathHelper.lerp(partialTicks, stalkingEntity.prevPosZ, stalkingEntity.getPosZ()));
        }
    }

    @Invoker
    abstract void callSetDirection(float pitch, float yaw);

    @Invoker
    abstract void callSetPosition(double x, double y, double z);
}