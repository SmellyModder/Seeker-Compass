package smelly.seekercompass.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.LocatableSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smelly.seekercompass.SeekerCompass;
import smelly.seekercompass.Stalker;

import javax.annotation.Nullable;
import java.util.Random;

@Mixin(ClientPlayerEntity.class)
public final class ClientPlayerEntityMixin implements Stalker {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final ResourceLocation STALKING_SHADER = new ResourceLocation(SeekerCompass.MOD_ID, "shaders/post/seeker.json");

    private LivingEntity stalkingEntity;
    private boolean wasHidingGUI;
    private boolean hasUnstalked;

    @Inject(at = @At("HEAD"), method = "isCurrentViewEntity", cancellable = true)
    private void isCurrentViewEntity(CallbackInfoReturnable<Boolean> info) {
        if (((Stalker) (Object) this).getStalkingEntity() != null) info.setReturnValue(false);
    }

    @Override
    public void setStalkingEntity(@Nullable LivingEntity stalkingEntity) {
        if (this.stalkingEntity != stalkingEntity) {
            MC.getSoundHandler().play(new StalkingSound());
        }

        this.stalkingEntity = stalkingEntity;
        if (stalkingEntity != null) {
            this.wasHidingGUI = MC.gameSettings.hideGUI;
            MC.gameRenderer.loadShader(STALKING_SHADER);
            MC.ingameGUI.setOverlayMessage(new TranslationTextComponent("message.seeker_compass.stalking_controls"), false);
            this.hasUnstalked = false;
        } else {
            ShaderGroup shaderGroup =  MC.gameRenderer.getShaderGroup();
            if (shaderGroup != null) {
                MC.gameRenderer.stopUseShader();
            }
            boolean wasHidingGUI = this.wasHidingGUI;
            if (wasHidingGUI != MC.gameSettings.hideGUI) {
                MC.gameSettings.hideGUI = wasHidingGUI;
            }
            this.hasUnstalked = true;
        }
    }

    @Nullable
    @Override
    public LivingEntity getStalkingEntity() {
        return this.stalkingEntity;
    }

    @Override
    public boolean wasHidingGUI() {
        return this.wasHidingGUI;
    }

    @Override
    public void setUnstalked(boolean hasUnstalked) {
        this.hasUnstalked = hasUnstalked;
    }

    @Override
    public boolean hasUnstalked() {
        return this.hasUnstalked;
    }

    static class StalkingSound extends LocatableSound {

        StalkingSound() {
            super(SoundEvents.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS);
            this.repeat = false;
            this.volume = 0.5F;
            this.pitch =  0.75F - (new Random().nextFloat() * 0.1F);
            this.priority = true;
            this.global = true;
        }

    }
}