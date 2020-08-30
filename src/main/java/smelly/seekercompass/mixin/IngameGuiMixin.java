package smelly.seekercompass.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import smelly.seekercompass.Stalker;

@Mixin(IngameGui.class)
public final class IngameGuiMixin {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final TranslationTextComponent UNSTALKING_MESSAGE = new TranslationTextComponent("stalking.seeker_compass.overlay_message");

    @Inject(at = @At("HEAD"), method = "getRenderViewPlayer", cancellable = true)
    private void getRenderViewPlayer(CallbackInfoReturnable<PlayerEntity> info) {
        if (((Stalker) MC.player).getStalkingEntity() != null) info.setReturnValue(null);
    }
}
