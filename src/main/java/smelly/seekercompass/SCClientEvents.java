package smelly.seekercompass;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import smelly.seekercompass.network.MessageC2SResetStalker;

@Mod.EventBusSubscriber(modid = SeekerCompass.MOD_ID, value = Dist.CLIENT)
public final class SCClientEvents {
    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.MouseInputEvent event) {
        ClientPlayerEntity player = MC.player;
        Stalker stalker = (Stalker) player;
        if (MC.currentScreen == null && event.getAction() == 1 && stalker != null && stalker.getStalkingEntity() != null) {
            stalker.setStalkingEntity(null);
            SeekerCompass.CHANNEL.sendToServer(new MessageC2SResetStalker(player.getEntityId()));
        }
    }
}