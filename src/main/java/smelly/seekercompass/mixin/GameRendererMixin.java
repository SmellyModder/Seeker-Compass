package smelly.seekercompass.mixin;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import smelly.seekercompass.Stalker;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {
    private static final Minecraft MC = Minecraft.getInstance();

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/GameSettings;hideGUI:Z", ordinal = 0), method = "updateCameraAndRender")
    private boolean updateCameraAndRender(GameSettings gameSettings, float partialTicks, long nanoTime, boolean renderWorldIn) {
        //Logic is inverted.
        if (gameSettings.hideGUI) {
            if (((Stalker) MC.player).getStalkingEntity() != null) {
                return false;
            }
            return true;
        }
        return false;
    }
}