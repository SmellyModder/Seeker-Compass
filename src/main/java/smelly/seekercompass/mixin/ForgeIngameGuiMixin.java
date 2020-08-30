package smelly.seekercompass.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import smelly.seekercompass.Stalker;

@Mixin(ForgeIngameGui.class)
public final class ForgeIngameGuiMixin {
    private static final Minecraft MC = Minecraft.getInstance();

    @Redirect(at = @At(value = "FIELD", target = "Lnet/minecraft/client/GameSettings;hideGUI:Z", ordinal = 2), method = "func_238445_a_")
    private boolean renderGameOverlay(GameSettings gameSettings, MatrixStack stack, float partialTicks) {
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
